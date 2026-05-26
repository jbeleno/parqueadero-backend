package com.usco.parqueaderos_api.report.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Metricas operacionales (no facturacion): duracion de estadia, ocupacion horaria,
 * vehiculos unicos, picos del dia.
 */
@Service
@RequiredArgsConstructor
public class ReporteOperacionService {

    @PersistenceContext
    private EntityManager em;

    private final CurrentUserService currentUser;

    /**
     * Percentiles de duracion de estadia (CERRADO) en minutos.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> duracionEstadia(Long parqueaderoId, LocalDate desde, LocalDate hasta) {
        requireAdmin();
        LocalDateTime ini = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(23, 59, 59);

        String sql = "SELECT " +
                "  percentile_cont(0.50) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (fecha_hora_salida - fecha_hora_entrada))/60) AS p50, " +
                "  percentile_cont(0.75) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (fecha_hora_salida - fecha_hora_entrada))/60) AS p75, " +
                "  percentile_cont(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (fecha_hora_salida - fecha_hora_entrada))/60) AS p95, " +
                "  AVG(EXTRACT(EPOCH FROM (fecha_hora_salida - fecha_hora_entrada))/60) AS promedio, " +
                "  COUNT(*) AS tickets " +
                "FROM ticket " +
                "WHERE parqueadero_id = :parqueaderoId " +
                "  AND estado = 'CERRADO' " +
                "  AND fecha_hora_entrada BETWEEN :ini AND :fin " +
                "  AND fecha_hora_salida IS NOT NULL";
        Object[] row = (Object[]) em.createNativeQuery(sql)
                .setParameter("parqueaderoId", parqueaderoId)
                .setParameter("ini", ini)
                .setParameter("fin", fin)
                .getSingleResult();

        Map<String, Object> out = new HashMap<>();
        out.put("parqueaderoId", parqueaderoId);
        out.put("desde", desde.toString());
        out.put("hasta", hasta.toString());
        out.put("p50Minutos", row[0] != null ? ((Number) row[0]).doubleValue() : 0.0);
        out.put("p75Minutos", row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
        out.put("p95Minutos", row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
        out.put("promedioMinutos", row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
        out.put("ticketsAnalizados", row[4] != null ? ((Number) row[4]).intValue() : 0);
        return out;
    }

    /**
     * Ocupacion por hora del dia: cuantos tickets se crearon en cada hora
     * y % ocupacion promedio = entradas_hora / capacidad_parqueadero.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ocupacionHoraria(Long parqueaderoId, LocalDate fecha) {
        requireAdmin();
        LocalDateTime ini = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);
        String sql = "SELECT EXTRACT(HOUR FROM fecha_hora_entrada) AS h, COUNT(*) AS tickets " +
                "FROM ticket " +
                "WHERE parqueadero_id = :parqueaderoId " +
                "  AND fecha_hora_entrada BETWEEN :ini AND :fin " +
                "GROUP BY EXTRACT(HOUR FROM fecha_hora_entrada) " +
                "ORDER BY h";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("parqueaderoId", parqueaderoId)
                .setParameter("ini", ini)
                .setParameter("fin", fin)
                .getResultList();

        // Capacidad para %
        Number capacidadNum = (Number) em.createNativeQuery(
                "SELECT COALESCE(numero_puntos_parqueo, 0) FROM parqueadero WHERE id = :id")
                .setParameter("id", parqueaderoId)
                .getSingleResult();
        int capacidad = capacidadNum != null ? capacidadNum.intValue() : 0;

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> bucket = new HashMap<>();
            int hora = ((Number) r[0]).intValue();
            int tickets = ((Number) r[1]).intValue();
            bucket.put("hora", hora);
            bucket.put("tickets", tickets);
            bucket.put("ocupacionPct", capacidad > 0 ? Math.min(1.0, (double) tickets / capacidad) : 0.0);
            result.add(bucket);
        }
        return result;
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo ADMIN/SUPER_ADMIN");
        }
    }
}
