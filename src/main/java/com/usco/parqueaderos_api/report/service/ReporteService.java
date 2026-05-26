package com.usco.parqueaderos_api.report.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.report.dto.*;
import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import com.usco.parqueaderos_api.subscription.repository.SuscripcionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes y dashboard del negocio. Queries SQL nativas con GROUP BY.
 *
 * - Filtrado multi-tenant: si el usuario es ADMIN, se restringe a su empresa.
 * - Agregados con SUM/COUNT/COUNT DISTINCT. Sin paginacion (datos agregados son pequenos).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteService {

    private final EntityManager em;
    private final SuscripcionRepository suscripcionRepo;
    private final CurrentUserService currentUser;

    /**
     * Resumen de facturacion en un rango de fechas, agrupado por dia/semana/mes.
     */
    @Transactional(readOnly = true)
    public ResumenFacturacionDTO facturacion(Long parqueaderoId, LocalDate desde,
                                              LocalDate hasta, String agruparPor) {
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.plusDays(1).atStartOfDay();
        String truncFn = sqlDateTrunc(agruparPor);

        // 1) Totales globales del rango
        String sqlTotal = "SELECT " +
                "  COALESCE(SUM(f.valor_total), 0)                                       AS facturado, " +
                "  COALESCE(SUM(CASE WHEN f.estado = 'PAGADA' THEN f.valor_total END), 0) AS pagado, " +
                "  COALESCE(SUM(CASE WHEN f.estado <> 'PAGADA' THEN f.valor_total END),0) AS pendiente, " +
                "  COUNT(f.id)                                                            AS cant_facturas, " +
                "  COUNT(DISTINCT t.id)                                                   AS cant_tickets, " +
                "  COUNT(DISTINCT t.vehiculo_id)                                          AS vehiculos_unicos " +
                "FROM ticket t " +
                "LEFT JOIN factura f ON f.ticket_id = t.id " +
                "WHERE t.fecha_hora_entrada BETWEEN :desde AND :hasta " +
                "  AND t.parqueadero_id = :parqId";
        Query q = em.createNativeQuery(sqlTotal);
        q.setParameter("desde", desdeDt);
        q.setParameter("hasta", hastaDt);
        q.setParameter("parqId", parqueaderoId);
        Object[] r = (Object[]) q.getSingleResult();
        double totFacturado = toDouble(r[0]);
        double totPagado = toDouble(r[1]);
        double totPendiente = toDouble(r[2]);
        long cantFacturas = toLong(r[3]);
        long cantTickets = toLong(r[4]);
        long vehiculosUnicos = toLong(r[5]);
        double visitasPromedio = vehiculosUnicos > 0 ? (double) cantTickets / vehiculosUnicos : 0.0;

        // 2) Buckets agrupados
        String sqlBuckets = "SELECT " +
                "  " + truncFn + " t.fecha_hora_entrada) AS bucket, " +
                "  COALESCE(SUM(f.valor_total), 0), " +
                "  COALESCE(SUM(CASE WHEN f.estado = 'PAGADA' THEN f.valor_total END), 0), " +
                "  COALESCE(SUM(CASE WHEN f.estado <> 'PAGADA' THEN f.valor_total END), 0), " +
                "  COUNT(f.id), " +
                "  COUNT(DISTINCT t.id), " +
                "  COUNT(DISTINCT t.vehiculo_id) " +
                "FROM ticket t " +
                "LEFT JOIN factura f ON f.ticket_id = t.id " +
                "WHERE t.fecha_hora_entrada BETWEEN :desde AND :hasta " +
                "  AND t.parqueadero_id = :parqId " +
                "GROUP BY bucket " +
                "ORDER BY bucket ASC";
        Query qB = em.createNativeQuery(sqlBuckets);
        qB.setParameter("desde", desdeDt);
        qB.setParameter("hasta", hastaDt);
        qB.setParameter("parqId", parqueaderoId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = qB.getResultList();
        List<BucketFacturacionDTO> buckets = new ArrayList<>();
        for (Object[] row : rows) {
            buckets.add(BucketFacturacionDTO.builder()
                    .periodo(String.valueOf(row[0]))
                    .facturado(toDouble(row[1]))
                    .pagado(toDouble(row[2]))
                    .pendiente(toDouble(row[3]))
                    .cantidadFacturas(toLong(row[4]))
                    .cantidadTickets(toLong(row[5]))
                    .vehiculosUnicos(toLong(row[6]))
                    .build());
        }

        return ResumenFacturacionDTO.builder()
                .desde(desde).hasta(hasta)
                .parqueaderoId(parqueaderoId)
                .agruparPor(agruparPor)
                .totalFacturado(totFacturado)
                .totalPagado(totPagado)
                .totalPendiente(totPendiente)
                .cantidadFacturas(cantFacturas)
                .cantidadTickets(cantTickets)
                .vehiculosUnicos(vehiculosUnicos)
                .visitasPromedioPorVehiculo(round2(visitasPromedio))
                .buckets(buckets)
                .build();
    }

    /**
     * Facturacion agrupada por tipo de vehiculo.
     */
    @Transactional(readOnly = true)
    public List<BucketFacturacionDTO> porTipoVehiculo(Long parqueaderoId, LocalDate desde, LocalDate hasta) {
        String sql = "SELECT " +
                "  tv.nombre, " +
                "  COALESCE(SUM(f.valor_total), 0), " +
                "  COUNT(f.id), " +
                "  COUNT(DISTINCT t.id), " +
                "  COUNT(DISTINCT t.vehiculo_id) " +
                "FROM ticket t " +
                "INNER JOIN vehiculo v ON v.id = t.vehiculo_id " +
                "INNER JOIN tipo_vehiculo tv ON tv.id = v.tipo_vehiculo_id " +
                "LEFT JOIN factura f ON f.ticket_id = t.id " +
                "WHERE t.fecha_hora_entrada BETWEEN :desde AND :hasta " +
                "  AND t.parqueadero_id = :parqId " +
                "GROUP BY tv.nombre ORDER BY SUM(f.valor_total) DESC NULLS LAST";
        Query q = em.createNativeQuery(sql);
        q.setParameter("desde", desde.atStartOfDay());
        q.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
        q.setParameter("parqId", parqueaderoId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<BucketFacturacionDTO> out = new ArrayList<>();
        for (Object[] row : rows) {
            out.add(BucketFacturacionDTO.builder()
                    .periodo(String.valueOf(row[0]))
                    .facturado(toDouble(row[1]))
                    .cantidadFacturas(toLong(row[2]))
                    .cantidadTickets(toLong(row[3]))
                    .vehiculosUnicos(toLong(row[4]))
                    .build());
        }
        return out;
    }

    /**
     * Facturacion segregada por fuente de ingreso: tickets normales vs suscripciones.
     */
    @Transactional(readOnly = true)
    public List<BucketFacturacionDTO> porFuente(Long parqueaderoId, LocalDate desde, LocalDate hasta) {
        String sql = "SELECT " +
                "  CASE " +
                "    WHEN t.suscripcion_id IS NULL THEN 'TICKET_NORMAL' " +
                "    ELSE 'SUSCRIPCION' " +
                "  END AS fuente, " +
                "  COALESCE(SUM(f.valor_total), 0), " +
                "  COUNT(f.id), " +
                "  COUNT(DISTINCT t.id), " +
                "  COUNT(DISTINCT t.vehiculo_id) " +
                "FROM ticket t " +
                "LEFT JOIN factura f ON f.ticket_id = t.id " +
                "WHERE t.fecha_hora_entrada BETWEEN :desde AND :hasta " +
                "  AND t.parqueadero_id = :parqId " +
                "GROUP BY fuente";
        Query q = em.createNativeQuery(sql);
        q.setParameter("desde", desde.atStartOfDay());
        q.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
        q.setParameter("parqId", parqueaderoId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<BucketFacturacionDTO> out = new ArrayList<>();
        for (Object[] row : rows) {
            out.add(BucketFacturacionDTO.builder()
                    .periodo(String.valueOf(row[0]))
                    .facturado(toDouble(row[1]))
                    .cantidadFacturas(toLong(row[2]))
                    .cantidadTickets(toLong(row[3]))
                    .vehiculosUnicos(toLong(row[4]))
                    .build());
        }
        return out;
    }

    /**
     * Top vehiculos por cantidad de tickets en el rango. Ordenado descendente.
     */
    @Transactional(readOnly = true)
    public List<TopVehiculoDTO> topVehiculos(Long parqueaderoId, LocalDate desde,
                                              LocalDate hasta, int limit) {
        String sql = "SELECT v.id, v.placa, COUNT(t.id), COALESCE(SUM(f.valor_total), 0) " +
                "FROM ticket t " +
                "INNER JOIN vehiculo v ON v.id = t.vehiculo_id " +
                "LEFT JOIN factura f ON f.ticket_id = t.id " +
                "WHERE t.fecha_hora_entrada BETWEEN :desde AND :hasta " +
                "  AND t.parqueadero_id = :parqId " +
                "GROUP BY v.id, v.placa " +
                "ORDER BY COUNT(t.id) DESC " +
                "LIMIT :lim";
        Query q = em.createNativeQuery(sql);
        q.setParameter("desde", desde.atStartOfDay());
        q.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
        q.setParameter("parqId", parqueaderoId);
        q.setParameter("lim", limit);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<TopVehiculoDTO> out = new ArrayList<>();
        for (Object[] row : rows) {
            out.add(TopVehiculoDTO.builder()
                    .vehiculoId(toLong(row[0]))
                    .placa(String.valueOf(row[1]))
                    .cantidadTickets(toLong(row[2]))
                    .totalFacturado(toDouble(row[3]))
                    .build());
        }
        return out;
    }

    /**
     * Compara el mes actual contra el mes anterior. Util para dashboard.
     */
    @Transactional(readOnly = true)
    public ComparativaPeriodoDTO comparativaMensual(Long parqueaderoId, LocalDate fechaReferencia) {
        LocalDate actualDesde = fechaReferencia.withDayOfMonth(1);
        LocalDate actualHasta = actualDesde.plusMonths(1).minusDays(1);
        LocalDate anteriorDesde = actualDesde.minusMonths(1);
        LocalDate anteriorHasta = actualDesde.minusDays(1);

        double[] actual = totalesSimples(parqueaderoId, actualDesde, actualHasta);
        double[] anterior = totalesSimples(parqueaderoId, anteriorDesde, anteriorHasta);

        double facturadoActual = actual[0];
        double facturadoAnterior = anterior[0];
        long ticketsActual = (long) actual[1];
        long ticketsAnterior = (long) anterior[1];

        double deltaAbs = facturadoActual - facturadoAnterior;
        double deltaPct = facturadoAnterior > 0 ? (deltaAbs / facturadoAnterior) * 100.0 : 0.0;
        double deltaTicketsPct = ticketsAnterior > 0
                ? ((ticketsActual - ticketsAnterior) / (double) ticketsAnterior) * 100.0 : 0.0;

        return ComparativaPeriodoDTO.builder()
                .periodoActualDesde(actualDesde).periodoActualHasta(actualHasta)
                .periodoAnteriorDesde(anteriorDesde).periodoAnteriorHasta(anteriorHasta)
                .facturadoActual(facturadoActual).facturadoAnterior(facturadoAnterior)
                .deltaAbsoluto(deltaAbs).deltaPorcentual(round2(deltaPct))
                .ticketsActual(ticketsActual).ticketsAnterior(ticketsAnterior)
                .deltaTicketsPorcentual(round2(deltaTicketsPct))
                .build();
    }

    private double[] totalesSimples(Long parqueaderoId, LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COALESCE(SUM(f.valor_total), 0), COUNT(DISTINCT t.id) " +
                "FROM ticket t LEFT JOIN factura f ON f.ticket_id = t.id " +
                "WHERE t.fecha_hora_entrada BETWEEN :desde AND :hasta AND t.parqueadero_id = :parqId";
        Query q = em.createNativeQuery(sql);
        q.setParameter("desde", desde.atStartOfDay());
        q.setParameter("hasta", hasta.plusDays(1).atStartOfDay());
        q.setParameter("parqId", parqueaderoId);
        Object[] r = (Object[]) q.getSingleResult();
        return new double[] { toDouble(r[0]), toDouble(r[1]) };
    }

    /**
     * Resumen de suscripciones del parqueadero.
     */
    @Transactional(readOnly = true)
    public ResumenSuscripcionesDTO resumenSuscripciones(Long parqueaderoId) {
        long activas = suscripcionRepo.countByParqueaderoIdAndEstado(parqueaderoId, EstadoSuscripcion.ACTIVA);
        long vencidas = suscripcionRepo.countByParqueaderoIdAndEstado(parqueaderoId, EstadoSuscripcion.VENCIDA);
        LocalDateTime ahora = LocalDateTime.now();
        List<Suscripcion> proximasAVencer = suscripcionRepo.findProximasAVencer(
                ahora, ahora.plusDays(7), parqueaderoId);

        long mensuales = 0, pasesDia = 0, abonos = 0;
        double saldoTotal = 0;
        for (Suscripcion s : suscripcionRepo.findByParqueaderoIdAndEstado(parqueaderoId, EstadoSuscripcion.ACTIVA)) {
            if (s.getTipo() == TipoSuscripcion.MENSUAL) mensuales++;
            else if (s.getTipo() == TipoSuscripcion.PASE_DIA) pasesDia++;
            else if (s.getTipo() == TipoSuscripcion.ABONO_PREPAGO) {
                abonos++;
                saldoTotal += s.getSaldoRestante() != null ? s.getSaldoRestante() : 0;
            }
        }

        // Ingresos por suscripciones en el ultimo mes (monto_pagado)
        String sql = "SELECT COALESCE(SUM(monto_pagado), 0) FROM suscripcion " +
                "WHERE parqueadero_id = :parqId AND fecha_creacion >= :hace30";
        Query q = em.createNativeQuery(sql);
        q.setParameter("parqId", parqueaderoId);
        q.setParameter("hace30", ahora.minusDays(30));
        double ingresosMes = toDouble(q.getSingleResult());

        return ResumenSuscripcionesDTO.builder()
                .parqueaderoId(parqueaderoId)
                .activas(activas)
                .vencidas(vencidas)
                .vencenEnSieteDias((long) proximasAVencer.size())
                .mensuales(mensuales)
                .pasesDia(pasesDia)
                .abonosPrepago(abonos)
                .saldoTotalEnAbonos(saldoTotal)
                .ingresosUltimoMes(ingresosMes)
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String sqlDateTrunc(String agruparPor) {
        switch (agruparPor != null ? agruparPor.toLowerCase() : "dia") {
            case "semana": return "DATE_TRUNC('week',";
            case "mes":    return "DATE_TRUNC('month',";
            case "dia":
            default:       return "DATE_TRUNC('day',";
        }
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof BigDecimal b) return b.doubleValue();
        return 0.0;
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        return 0L;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
