package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.listener.AutoFacturaListener;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Backfill admin de facturas faltantes para tickets CERRADOS con monto > 0
 * que no tengan factura asociada.
 *
 * Diseño conservador:
 *   - Solo SUPER_ADMIN.
 *   - Ventana de fechas obligatoria (no se permite "todo el historico" ciego).
 *   - dryRun=true devuelve la LISTA de candidatos sin crear nada.
 *   - Cada factura creada lleva origen="BACKFILL_<timestamp>" para trazabilidad.
 *   - Idempotente: una segunda corrida no duplica (proteccion por UNIQUE INDEX
 *     parcial en factura(ticket_id) WHERE estado != 'ANULADA').
 *   - La fechaHora de la factura es la fecha_hora_salida del ticket (preserva
 *     historia contable correcta), no now().
 *
 * Reversible: DELETE FROM factura WHERE origen LIKE 'BACKFILL_%'.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackfillFacturaService {

    private final TicketRepository ticketRepository;
    private final FacturaRepository facturaRepository;
    private final AutoFacturaListener autoFacturaListener;
    private final CurrentUserService currentUser;

    /** Resultado del dry-run o apply. */
    public record BackfillResult(
            boolean dryRun,
            String origenAplicado,
            Long parqueaderoId,
            LocalDateTime desde,
            LocalDateTime hasta,
            int candidatos,
            int creadas,
            int omitidos,
            List<Map<String, Object>> tickets) {}

    @Transactional
    public BackfillResult ejecutar(Long parqueaderoId, LocalDate desde, LocalDate hasta, boolean dryRun) {
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede ejecutar backfill");
        }
        if (desde == null || hasta == null) {
            throw new BusinessException(
                    "desde y hasta son obligatorios (sin rango no se permite)",
                    "ERR_MISSING_FIELDS");
        }
        if (hasta.isBefore(desde)) {
            throw new BusinessException("hasta debe ser >= desde", "ERR_INVALID_RANGE");
        }
        // Cap de seguridad: ventana max 90 dias
        if (desde.plusDays(90).isBefore(hasta)) {
            throw new BusinessException(
                    "ventana maxima permitida: 90 dias (ejecuta en lotes mas chicos)",
                    "ERR_INVALID_RANGE");
        }

        LocalDateTime ini = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);

        List<Ticket> candidatos = ticketRepository.findCerradosFacturables(parqueaderoId, ini, fin);

        // Filtrar a los que NO tienen factura no-anulada (precheck barato)
        List<Ticket> sinFactura = new ArrayList<>();
        for (Ticket t : candidatos) {
            boolean yaFactura = facturaRepository.findByTicketId(t.getId()).stream()
                    .anyMatch(f -> !"ANULADA".equals(f.getEstado()));
            if (!yaFactura) sinFactura.add(t);
        }

        String origenTag = "BACKFILL_" + LocalDateTime.now().toString().substring(0, 19).replace(":", "");
        int creadas = 0;
        int omitidos = 0;
        List<Map<String, Object>> detalle = new ArrayList<>();
        for (Ticket t : sinFactura) {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("ticketId", t.getId());
            row.put("placa", t.getVehiculo() != null ? t.getVehiculo().getPlaca() : null);
            row.put("parqueaderoId", t.getParqueadero() != null ? t.getParqueadero().getId() : null);
            row.put("fechaHoraEntrada", t.getFechaHoraEntrada() != null ? t.getFechaHoraEntrada().toString() : null);
            row.put("fechaHoraSalida", t.getFechaHoraSalida() != null ? t.getFechaHoraSalida().toString() : null);
            row.put("montoCalculado", t.getMontoCalculado());
            if (dryRun) {
                row.put("accion", "DRY_RUN_PROYECTADO");
            } else {
                Factura f = autoFacturaListener.generarFacturaSiCorresponde(
                        t, origenTag, t.getFechaHoraSalida());
                if (f != null) {
                    creadas++;
                    row.put("accion", "CREADA");
                    row.put("facturaId", f.getId());
                } else {
                    omitidos++;
                    row.put("accion", "OMITIDA");
                }
            }
            detalle.add(row);
        }
        log.info("Backfill {} parq={} rango={}..{}: candidatos={} creadas={} omitidos={}",
                dryRun ? "DRY_RUN" : "APPLY", parqueaderoId, desde, hasta,
                sinFactura.size(), creadas, omitidos);

        return new BackfillResult(
                dryRun,
                dryRun ? null : origenTag,
                parqueaderoId,
                ini,
                fin,
                sinFactura.size(),
                creadas,
                omitidos,
                detalle
        );
    }
}
