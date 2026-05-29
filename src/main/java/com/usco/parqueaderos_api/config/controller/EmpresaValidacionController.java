package com.usco.parqueaderos_api.config.controller;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.config.entity.EmpresaValidacionCampo;
import com.usco.parqueaderos_api.config.repository.EmpresaValidacionCampoRepository;
import com.usco.parqueaderos_api.config.service.EmpresaValidacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD de validaciones por empresa. v49 Fase 4.
 *
 * GET  /api/empresa-validaciones         → lista de MI empresa
 * GET  /api/empresa-validaciones/{entidad} → reglas de UNA entidad
 * PUT  /api/empresa-validaciones         → upsert regla (ADMIN/SUPER_ADMIN)
 */
@RestController
@RequestMapping("/api/empresa-validaciones")
@RequiredArgsConstructor
public class EmpresaValidacionController {

    private final EmpresaValidacionCampoRepository repo;
    private final EmpresaValidacionService service;
    private final CurrentUserService currentUser;

    private Long empresaIdOrFail() {
        return currentUser.getCurrentEmpresaId().orElseThrow(
                () -> new AccessDeniedException("Usuario sin empresa asociada"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmpresaValidacionCampo>>> listarMias() {
        return ResponseEntity.ok(ApiResponse.ok(
                repo.findByEmpresaIdAndActivaTrueOrderByEntidadAscCampoAsc(empresaIdOrFail()),
                "Validaciones por campo"));
    }

    @GetMapping("/{entidad}")
    public ResponseEntity<ApiResponse<List<EmpresaValidacionCampo>>> listarPorEntidad(@PathVariable String entidad) {
        return ResponseEntity.ok(ApiResponse.ok(
                repo.findByEmpresaIdAndEntidadAndActivaTrueOrderByCampoAsc(empresaIdOrFail(), entidad),
                "Validaciones de " + entidad));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<EmpresaValidacionCampo>> upsert(@RequestBody EmpresaValidacionCampo regla) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo ADMIN/SUPER_ADMIN puede modificar validaciones");
        }
        regla.setEmpresaId(empresaIdOrFail()); // forzar empresa del JWT
        if (regla.getEntidad() == null || regla.getCampo() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("entidad y campo son obligatorios"));
        }
        EmpresaValidacionCampo saved = service.upsert(regla, currentUser.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(saved, "Validacion actualizada"));
    }
}
