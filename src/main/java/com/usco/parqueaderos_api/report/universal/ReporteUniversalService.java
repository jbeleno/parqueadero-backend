package com.usco.parqueaderos_api.report.universal;

import com.usco.parqueaderos_api.audit.service.AuditService;
import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.report.universal.export.CsvExporter;
import com.usco.parqueaderos_api.report.universal.export.PdfExporter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio universal de reportes. Despacha al spec correcto segun la clave
 * (recurso) y formatea la salida en JSON, CSV o PDF.
 *
 * RBAC: cada spec declara sus roles permitidos. El service valida.
 * Multi-tenant: ADMIN se fuerza a su empresa, ADMIN_PARQUEADERO solo sus parqueaderos,
 * OPERARIO_CAJA solo su parqueadero actual.
 *
 * Cada generacion queda registrada en audit_log con accion "EXPORT".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteUniversalService {

    @PersistenceContext private EntityManager em;

    private final List<ReporteSpec> specs;
    private final CsvExporter csvExporter;
    private final PdfExporter pdfExporter;
    private final CurrentUserService currentUser;
    private final AuditService auditService;

    public enum Formato { JSON, CSV, PDF }

    /** Resultado del reporte. */
    public record Resultado(byte[] bytes, String contentType, String filename, int filas) {}

    @Transactional(readOnly = true)
    public Resultado generar(String clave, ReporteSpec.Filtros filtrosIn, Formato formato) {
        ReporteSpec spec = findSpec(clave);
        // RBAC
        if (!tieneRolPermitido(spec)) {
            throw new AccessDeniedException(
                    "No tienes rol con acceso a este reporte: " + clave);
        }

        // Aplicar filtros forzados segun rol multi-tenant
        ReporteSpec.Filtros filtros = ajustarFiltros(filtrosIn);

        List<Map<String, Object>> filas = spec.ejecutar(em, filtros);

        // Auditar la exportacion
        Map<String, Object> meta = new HashMap<>();
        meta.put("clave", clave);
        meta.put("formato", formato.name());
        meta.put("filas", filas.size());
        meta.put("filtros", filtros);
        auditService.log("reporte", null, "EXPORT", null, meta, filtros.empresaId(),
                "Generacion de reporte " + clave + " formato=" + formato);

        String correo = safeCorreo();
        String ts = java.time.LocalDateTime.now().toString().substring(0, 19).replace(":", "");
        switch (formato) {
            case CSV: {
                byte[] bytes = csvExporter.exportar(spec, filas);
                return new Resultado(bytes, "text/csv",
                        "reporte-" + clave + "-" + ts + ".csv", filas.size());
            }
            case PDF: {
                byte[] bytes = pdfExporter.exportar(spec, filas, filtros, correo);
                return new Resultado(bytes, "application/pdf",
                        "reporte-" + clave + "-" + ts + ".pdf", filas.size());
            }
            case JSON:
            default: {
                // Devolvemos las filas como mapa (el controller lo serializa con Jackson)
                return new Resultado(null, "application/json",
                        "reporte-" + clave + ".json", filas.size());
            }
        }
    }

    /** Para el endpoint JSON: devuelve directamente las filas. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> generarJson(String clave, ReporteSpec.Filtros filtrosIn) {
        ReporteSpec spec = findSpec(clave);
        if (!tieneRolPermitido(spec)) {
            throw new AccessDeniedException("No tienes rol con acceso a este reporte: " + clave);
        }
        ReporteSpec.Filtros filtros = ajustarFiltros(filtrosIn);
        List<Map<String, Object>> filas = spec.ejecutar(em, filtros);
        auditService.log("reporte", null, "EXPORT_JSON", null,
                Map.of("clave", clave, "filas", filas.size()), filtros.empresaId(), null);
        return filas;
    }

    /** Lista de reportes disponibles para el usuario actual. */
    public List<Map<String, String>> catalogo() {
        return specs.stream()
                .filter(this::tieneRolPermitido)
                .map(s -> Map.of("clave", s.clave(), "titulo", s.titulo()))
                .toList();
    }

    private ReporteSpec findSpec(String clave) {
        return specs.stream()
                .filter(s -> s.clave().equalsIgnoreCase(clave))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Reporte no existe: " + clave, "ERR_REPORTE_NO_EXISTE"));
    }

    private boolean tieneRolPermitido(ReporteSpec spec) {
        for (String rol : spec.rolesPermitidos()) {
            if (currentUser.hasRole(rol)) return true;
        }
        return false;
    }

    private ReporteSpec.Filtros ajustarFiltros(ReporteSpec.Filtros in) {
        Long empresaId = in.empresaId();
        Long parqueaderoId = in.parqueaderoId();

        if (!currentUser.isSuperAdmin()) {
            if (currentUser.isAdmin()) {
                // Forzar empresa del usuario
                empresaId = currentUser.getCurrentEmpresaId().orElse(empresaId);
            } else if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
                // Forzar a un parqueadero del usuario
                if (parqueaderoId == null) {
                    // Si no especifica, usar el primero asignado
                    List<Long> ids = currentUser.getParqueaderoIds();
                    if (!ids.isEmpty()) parqueaderoId = ids.get(0);
                } else if (!currentUser.getParqueaderoIds().contains(parqueaderoId)) {
                    throw new AccessDeniedException(
                            "Parqueadero " + parqueaderoId + " no esta en tu asignacion");
                }
            }
        }
        return new ReporteSpec.Filtros(parqueaderoId, empresaId, in.desde(), in.hasta(),
                in.limite(), in.usuarioFiltro());
    }

    private String safeCorreo() {
        try { return currentUser.getCurrent().getCorreo(); }
        catch (Exception e) { return null; }
    }
}
