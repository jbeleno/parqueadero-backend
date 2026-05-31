package com.usco.parqueaderos_api.report.service;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.report.entity.ReporteDefinicion;
import com.usco.parqueaderos_api.report.entity.ReporteEjecutado;
import com.usco.parqueaderos_api.report.repository.ReporteDefinicionRepository;
import com.usco.parqueaderos_api.report.repository.ReporteEjecutadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Ejecuta reportes parametrizables (v49 Fase 8).
 *
 * Uso desde controller:
 * <pre>
 *   var result = service.ejecutar("tickets_por_dia",
 *       Map.of("parqueaderoId", 7L, "desde", t1, "hasta", t2),
 *       empresaId, usuarioId);
 * </pre>
 *
 * Seguridad:
 *  - Solo SELECT (validacion sobre sql_template).
 *  - Parametros nombrados (no concatenacion → no SQL injection).
 *  - Limit hard cap: el maxFilas del reporte. Si excede, se trunca y se
 *    avisa al cliente.
 *  - Registra cada ejecucion en reporte_ejecutado.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReporteParametrizableService {

    private final ReporteDefinicionRepository definicionRepo;
    private final ReporteEjecutadoRepository ejecutadoRepo;
    private final NamedParameterJdbcTemplate jdbc;

    @Transactional
    public Map<String, Object> ejecutar(String clave, Map<String, Object> parametros,
                                          Long empresaId, Long usuarioId) {
        long start = System.currentTimeMillis();

        ReporteDefinicion def = definicionRepo
                .findByClaveConFallbackGlobal(clave, empresaId)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ReporteDefinicion no encontrado: " + clave));

        String sql = def.getSqlTemplate().trim();
        if (!sql.toLowerCase().startsWith("select")) {
            throw new BusinessException(
                    "El reporte solo puede contener SELECT (recibido: " + sql.substring(0, 30) + "...)",
                    "ERR_REPORT_UNSAFE");
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (parametros != null) {
            // v49: parsear strings de timestamp/date a LocalDateTime/LocalDate
            // antes de pasarlos al JDBC. Sin esto PostgreSQL falla con "bad SQL
            // grammar" porque trata el string como text y no puede compararlo
            // con una columna timestamp.
            parametros.forEach((k, v) -> {
                if (v instanceof String s && esTimestampString(s)) {
                    try {
                        params.addValue(k, java.time.LocalDateTime.parse(s));
                        return;
                    } catch (Exception ignored) { /* deja como string */ }
                }
                if (v instanceof String s2 && esDateString(s2)) {
                    try {
                        params.addValue(k, java.time.LocalDate.parse(s2));
                        return;
                    } catch (Exception ignored) { /* deja como string */ }
                }
                params.addValue(k, v);
            });
        }

        List<Map<String, Object>> filas;
        ReporteEjecutado log = new ReporteEjecutado();
        log.setReporteDefinicionId(def.getId());
        log.setClaveReporte(clave);
        log.setEmpresaId(empresaId);
        log.setParametrosJson(parametros == null ? "{}" : parametros.toString());
        log.setEjecutadoPorUsuarioId(usuarioId);
        log.setFechaHora(LocalDateTime.now());

        try {
            filas = jdbc.queryForList(sql, params);
        } catch (Exception ex) {
            log.setEstado("ERROR");
            log.setErrorMensaje(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            log.setDuracionMs(System.currentTimeMillis() - start);
            try { ejecutadoRepo.save(log); } catch (Exception ignore) {}
            throw new BusinessException("Error ejecutando reporte: " + ex.getMessage(), "ERR_REPORT_FAIL");
        }

        boolean truncado = false;
        if (filas.size() > def.getMaxFilas()) {
            filas = filas.subList(0, def.getMaxFilas());
            truncado = true;
        }
        log.setFilasDevueltas(filas.size());
        log.setDuracionMs(System.currentTimeMillis() - start);
        ejecutadoRepo.save(log);

        return Map.of(
                "clave", clave,
                "nombre", def.getNombre(),
                "filas", filas,
                "totalFilas", filas.size(),
                "truncado", truncado,
                "duracionMs", log.getDuracionMs());
    }

    private static boolean esTimestampString(String s) {
        // Acepta "2026-01-01T00:00:00" o con milisegundos
        return s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?$");
    }

    private static boolean esDateString(String s) {
        return s.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }
}
