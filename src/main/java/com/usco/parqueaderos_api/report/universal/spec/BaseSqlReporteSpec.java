package com.usco.parqueaderos_api.report.universal.spec;

import com.usco.parqueaderos_api.report.universal.ReporteSpec;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper para specs que ejecutan una consulta SQL nativa con filtros
 * estandar (parqueaderoId, empresaId, desde, hasta, limite).
 *
 * La SQL debe usar placeholders nombrados :parqueaderoId, :empresaId, :desde, :hasta.
 * Todos son opcionales — el helper detecta cuales se usan.
 */
public abstract class BaseSqlReporteSpec implements ReporteSpec {

    protected abstract String sqlBase();

    @Override
    public List<Map<String, Object>> ejecutar(EntityManager em, Filtros f) {
        String sql = sqlBase();
        var q = em.createNativeQuery(sql);
        if (sql.contains(":parqueaderoId")) q.setParameter("parqueaderoId", f.parqueaderoId());
        if (sql.contains(":empresaId")) q.setParameter("empresaId", f.empresaId());
        if (sql.contains(":desde")) {
            q.setParameter("desde", f.desde() != null ? f.desde().atStartOfDay() : null);
        }
        if (sql.contains(":hasta")) {
            q.setParameter("hasta", f.hasta() != null ? f.hasta().atTime(23, 59, 59) : null);
        }
        if (f.limite() != null) {
            q.setMaxResults(Math.min(5000, Math.max(1, f.limite())));
        } else {
            q.setMaxResults(5000); // cap absoluto para no agotar memoria
        }
        @SuppressWarnings("unchecked")
        List<Object[]> raw = q.getResultList();

        List<String> cols = new ArrayList<>(columnas().keySet());
        List<Map<String, Object>> out = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (int i = 0; i < cols.size() && i < row.length; i++) {
                Object v = row[i];
                if (v instanceof LocalDateTime ldt) v = ldt.toString();
                if (v instanceof java.sql.Timestamp ts) v = ts.toLocalDateTime().toString();
                if (v instanceof java.sql.Date d) v = d.toLocalDate().toString();
                m.put(cols.get(i), v);
            }
            out.add(m);
        }
        return out;
    }
}
