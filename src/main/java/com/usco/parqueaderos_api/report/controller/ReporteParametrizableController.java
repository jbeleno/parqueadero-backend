package com.usco.parqueaderos_api.report.controller;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.report.entity.ReporteDefinicion;
import com.usco.parqueaderos_api.report.entity.ReporteEjecutado;
import com.usco.parqueaderos_api.report.repository.ReporteDefinicionRepository;
import com.usco.parqueaderos_api.report.repository.ReporteEjecutadoRepository;
import com.usco.parqueaderos_api.report.service.ReporteParametrizableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints de reportes parametrizables. v49 Fase 8.
 *
 *  GET  /api/reportes-parametrizables           → lista de disponibles
 *  POST /api/reportes-parametrizables/{clave}/ejecutar → ejecuta con params
 *  GET  /api/reportes-historial                 → ejecuciones pasadas
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReporteParametrizableController {

    private final ReporteDefinicionRepository definicionRepo;
    private final ReporteEjecutadoRepository ejecutadoRepo;
    private final ReporteParametrizableService service;
    private final CurrentUserService currentUser;

    @GetMapping("/reportes-parametrizables")
    public ResponseEntity<ApiResponse<List<ReporteDefinicion>>> listarDisponibles() {
        Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(
                definicionRepo.findDisponiblesPara(empresaId),
                "Reportes disponibles"));
    }

    @PostMapping("/reportes-parametrizables/{clave}/ejecutar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ejecutar(
            @PathVariable String clave,
            @RequestBody(required = false) Map<String, Object> parametros) {
        Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
        Long uid = currentUser.getCurrentUserId();
        Map<String, Object> result = service.ejecutar(
                clave,
                parametros == null ? Map.of() : parametros,
                empresaId, uid);
        return ResponseEntity.ok(ApiResponse.ok(result, "Reporte ejecutado"));
    }

    @GetMapping("/reportes-historial")
    public ResponseEntity<ApiResponse<List<ReporteEjecutado>>> historial() {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo ADMIN/SUPER_ADMIN puede ver historial");
        }
        Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
        if (currentUser.isSuperAdmin() && empresaId == null) {
            return ResponseEntity.ok(ApiResponse.ok(
                    ejecutadoRepo.findAll(),
                    "Historial global"));
        }
        return ResponseEntity.ok(ApiResponse.ok(
                ejecutadoRepo.findTop100ByEmpresaIdOrderByFechaHoraDesc(empresaId),
                "Ultimas 100 ejecuciones"));
    }
}
