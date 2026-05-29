package com.usco.parqueaderos_api.config.controller;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.config.entity.EmpresaConfig;
import com.usco.parqueaderos_api.config.repository.EmpresaConfigRepository;
import com.usco.parqueaderos_api.config.service.EmpresaConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CRUD de configuracion por empresa. v49 Fase 3.
 *
 * - GET  /api/empresa-config              → lista la config de MI empresa
 * - GET  /api/empresa-config/{empresaId}  → lista config de empresa (ADMIN/SUPER_ADMIN)
 * - PUT  /api/empresa-config              → upsert clave/valor
 */
@RestController
@RequestMapping("/api/empresa-config")
@RequiredArgsConstructor
public class EmpresaConfigController {

    private final EmpresaConfigRepository repo;
    private final EmpresaConfigService service;
    private final CurrentUserService currentUser;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmpresaConfig>>> listarMia() {
        Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
        if (empresaId == null) {
            throw new AccessDeniedException("Usuario sin empresa asociada");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                repo.findByEmpresaIdOrderByCategoriaAscClaveAsc(empresaId),
                "Configuracion de empresa " + empresaId));
    }

    @GetMapping("/{empresaId}")
    public ResponseEntity<ApiResponse<List<EmpresaConfig>>> listarPorEmpresa(@PathVariable Long empresaId) {
        if (!currentUser.isSuperAdmin()) {
            currentUser.requireEmpresa(empresaId);
            if (!currentUser.isAdmin()) {
                throw new AccessDeniedException("Solo ADMIN/SUPER_ADMIN puede listar config de otra empresa");
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(
                repo.findByEmpresaIdOrderByCategoriaAscClaveAsc(empresaId),
                "Configuracion"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<EmpresaConfig>> upsert(@RequestBody Map<String, String> body) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo ADMIN/SUPER_ADMIN puede modificar config");
        }
        Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
        if (empresaId == null) {
            throw new AccessDeniedException("Usuario sin empresa asociada");
        }
        String clave = body.get("clave");
        String valor = body.get("valor");
        if (clave == null || clave.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("clave es obligatoria"));
        }
        EmpresaConfig saved = service.upsert(empresaId, clave, valor, currentUser.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(saved, "Configuracion actualizada"));
    }
}
