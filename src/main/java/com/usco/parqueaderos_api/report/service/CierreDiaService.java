package com.usco.parqueaderos_api.report.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.notification.dto.NotificacionDTO;
import com.usco.parqueaderos_api.notification.service.NotificationService;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.report.dto.CierreDiaDTO;
import com.usco.parqueaderos_api.report.entity.CierreDia;
import com.usco.parqueaderos_api.report.repository.CierreDiaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CierreDiaService {

    @PersistenceContext
    private EntityManager em;

    private final CierreDiaRepository repository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final CurrentUserService currentUser;
    private final NotificationService notificationService;

    /**
     * Genera CierreDia para todos los parqueaderos activos. Idempotente:
     * si ya existe (parqueadero, fecha), no lo recrea. Corre 23:55 diario.
     */
    @Scheduled(cron = "${app.cierre-dia.cron:0 55 23 * * *}")
    @Transactional
    public void generarCierresDeHoy() {
        LocalDate hoy = LocalDate.now();
        List<Parqueadero> todos = parqueaderoRepository.findAll();
        log.info("CierreDiaService: generando cierre del {} para {} parqueaderos", hoy, todos.size());
        for (Parqueadero p : todos) {
            generarCierreEntity(p.getId(), hoy);
        }
    }

    /**
     * Genera (o reemplaza si forzar=true) el cierre de un parqueadero para una fecha.
     */
    @Transactional
    public CierreDiaDTO generarCierre(Long parqueaderoId, LocalDate fecha) {
        return generarCierre(parqueaderoId, fecha, false);
    }

    /**
     * Si forzar=true, borra el CierreDia existente y lo recalcula. Util cuando
     * se hacen backfills de facturas y el cierre del dia quedo desactualizado.
     */
    @Transactional
    public CierreDiaDTO generarCierre(Long parqueaderoId, LocalDate fecha, boolean forzar) {
        if (forzar) {
            repository.findByParqueaderoIdAndFecha(parqueaderoId, fecha)
                    .ifPresent(existing -> {
                        log.info("CierreDia forzar=true: borrando cierre #{} para regenerar",
                                existing.getId());
                        repository.delete(existing);
                        repository.flush();
                    });
        }
        return toDTO(generarCierreEntity(parqueaderoId, fecha));
    }

    @Transactional
    public CierreDia generarCierreEntity(Long parqueaderoId, LocalDate fecha) {
        Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        // Idempotencia: si ya hay cierre del dia, no lo regenero
        if (repository.findByParqueaderoIdAndFecha(parqueaderoId, fecha).isPresent()) {
            log.debug("CierreDia ya existe para parq={} fecha={}, skip", parqueaderoId, fecha);
            return repository.findByParqueaderoIdAndFecha(parqueaderoId, fecha).get();
        }

        LocalDateTime ini = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        // Tickets cerrados y anulados
        Number ticketsCerrados = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM ticket WHERE parqueadero_id = :pid AND estado = 'CERRADO' " +
                "AND fecha_hora_salida BETWEEN :ini AND :fin")
                .setParameter("pid", parqueaderoId)
                .setParameter("ini", ini).setParameter("fin", fin).getSingleResult();
        Number ticketsAnulados = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM ticket WHERE parqueadero_id = :pid AND estado = 'ANULADO' " +
                "AND anulado_en BETWEEN :ini AND :fin")
                .setParameter("pid", parqueaderoId)
                .setParameter("ini", ini).setParameter("fin", fin).getSingleResult();

        // Pagos COMPLETADO del dia agrupados por metodo
        @SuppressWarnings("unchecked")
        List<Object[]> pagosPorMetodo = em.createNativeQuery(
                "SELECT pa.metodo, COALESCE(SUM(pa.monto),0) FROM pago pa " +
                "JOIN factura f ON f.id = pa.factura_id " +
                "WHERE f.parqueadero_id = :pid AND pa.estado = 'COMPLETADO' " +
                "AND pa.fecha_hora BETWEEN :ini AND :fin " +
                "GROUP BY pa.metodo")
                .setParameter("pid", parqueaderoId)
                .setParameter("ini", ini).setParameter("fin", fin).getResultList();
        double efectivo = 0, tarjeta = 0, otros = 0, totalCobrado = 0;
        for (Object[] r : pagosPorMetodo) {
            String m = (String) r[0];
            double monto = ((Number) r[1]).doubleValue();
            totalCobrado += monto;
            if ("EFECTIVO".equals(m)) efectivo += monto;
            else if (m != null && m.startsWith("TARJETA")) tarjeta += monto;
            else otros += monto;
        }

        // Facturas emitidas y pendientes
        Number facturasEmitidas = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM factura WHERE parqueadero_id = :pid " +
                "AND fecha_hora BETWEEN :ini AND :fin")
                .setParameter("pid", parqueaderoId)
                .setParameter("ini", ini).setParameter("fin", fin).getSingleResult();
        Number totalPendiente = (Number) em.createNativeQuery(
                "SELECT COALESCE(SUM(valor_total),0) FROM factura WHERE parqueadero_id = :pid " +
                "AND estado = 'PENDIENTE' AND fecha_hora BETWEEN :ini AND :fin")
                .setParameter("pid", parqueaderoId)
                .setParameter("ini", ini).setParameter("fin", fin).getSingleResult();

        CierreDia c = new CierreDia();
        c.setParqueadero(p);
        c.setFecha(fecha);
        c.setTicketsCerrados(ticketsCerrados.intValue());
        c.setTicketsAnulados(ticketsAnulados.intValue());
        c.setTotalCobrado(totalCobrado);
        c.setTotalEfectivo(efectivo);
        c.setTotalTarjeta(tarjeta);
        c.setTotalOtros(otros);
        c.setFacturasEmitidas(facturasEmitidas.intValue());
        c.setTotalPendiente(totalPendiente.doubleValue());
        c.setGeneradoEn(LocalDateTime.now());
        CierreDia saved = repository.save(c);

        // Notificar al operador del parqueadero
        Map<String, Object> data = new HashMap<>();
        data.put("cierreId", saved.getId());
        data.put("ticketsCerrados", saved.getTicketsCerrados());
        data.put("totalCobrado", saved.getTotalCobrado());
        data.put("totalEfectivo", saved.getTotalEfectivo());
        data.put("totalPendiente", saved.getTotalPendiente());
        NotificacionDTO n = NotificacionDTO.builder()
                .tipo("CIERRE_DIA")
                .mensaje("Cierre del " + fecha + ": $" + String.format("%,.0f", totalCobrado) + " cobrados, "
                        + ticketsCerrados.intValue() + " tickets")
                .parqueaderoId(parqueaderoId)
                .referenciaId(saved.getId())
                .data(data)
                .build();
        notificationService.notificarParqueadero(parqueaderoId, n);
        return saved;
    }

    @Transactional(readOnly = true)
    public CierreDiaDTO obtener(Long parqueaderoId, LocalDate fecha) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo ADMIN/SUPER_ADMIN");
        }
        return repository.findByParqueaderoIdAndFecha(parqueaderoId, fecha)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("CierreDia parq=" + parqueaderoId, fecha.toEpochDay()));
    }

    /** Mapea entity a DTO plano (evita serializar lazy proxies de Parqueadero.ciudad.departamento...). */
    private CierreDiaDTO toDTO(CierreDia c) {
        return new CierreDiaDTO(
                c.getId(),
                c.getParqueadero() != null ? c.getParqueadero().getId() : null,
                c.getParqueadero() != null ? c.getParqueadero().getNombre() : null,
                c.getFecha(),
                c.getTicketsCerrados(),
                c.getTotalCobrado(),
                c.getTotalEfectivo(),
                c.getTotalTarjeta(),
                c.getTotalOtros(),
                c.getFacturasEmitidas(),
                c.getTotalPendiente(),
                c.getTicketsAnulados(),
                c.getGeneradoEn()
        );
    }
}
