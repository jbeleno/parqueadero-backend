package com.usco.parqueaderos_api.catalog.global.controller;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.global.entity.EmpresaCatalogoGlobalActivo;
import com.usco.parqueaderos_api.catalog.global.service.EmpresaCatalogoService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints para que cada empresa configure cuales items globales acepta.
 * v50 Sprint 4.
 *
 * Por ejemplo, si la empresa A no quiere aceptar el tipo_documento "PEP"
 * para sus personas, marca activo=false en la fila correspondiente.
 *
 * Si la empresa NO tiene NINGUNA fila para un catalogo, por default acepta
 * TODOS los items globales (retrocompat).
 */
@RestController
@RequestMapping("/api/empresa-catalogos")
@RequiredArgsConstructor
public class EmpresaCatalogoController {

    private final EmpresaCatalogoService service;
    private final CurrentUserService currentUser;

    private Long requireEmpresaId() {
        return currentUser.getCurrentEmpresaId().orElseThrow(
                () -> new AccessDeniedException("Usuario sin empresa asociada"));
    }

    private void requireAdmin() {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException(
                    "Solo ADMIN/SUPER_ADMIN puede configurar los catalogos por empresa");
        }
    }

    @GetMapping("/{catalogo}")
    public ResponseEntity<ApiResponse<List<EmpresaCatalogoGlobalActivo>>> listar(
            @PathVariable String catalogo) {
        Long empresaId = requireEmpresaId();
        return ResponseEntity.ok(ApiResponse.ok(
                service.listarConfig(empresaId, catalogo),
                "Configuracion de " + catalogo + " de mi empresa"));
    }

    /**
     * Marca un item global como activo/inactivo para esta empresa.
     * Body: {itemId: Long, activo: boolean}
     */
    @PutMapping("/{catalogo}")
    public ResponseEntity<ApiResponse<EmpresaCatalogoGlobalActivo>> upsert(
            @PathVariable String catalogo,
            @RequestBody Map<String, Object> body) {
        requireAdmin();
        Long empresaId = requireEmpresaId();
        Long itemId = asLong(body.get("itemId"));
        Boolean activo = body.get("activo") == null ? Boolean.TRUE : (Boolean) body.get("activo");
        if (itemId == null) {
            throw new BusinessException("itemId es obligatorio", "ERR_MISSING_FIELDS");
        }
        EmpresaCatalogoGlobalActivo saved = service.upsert(
                empresaId, catalogo, itemId, activo, currentUser.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(saved, "Configuracion actualizada"));
    }

    @PostMapping("/{catalogo}/aceptar-todos")
    public ResponseEntity<ApiResponse<Integer>> aceptarTodos(@PathVariable String catalogo) {
        requireAdmin();
        Long empresaId = requireEmpresaId();
        int n = service.aceptarTodos(empresaId, catalogo, currentUser.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(n, "Activados " + n + " items globales"));
    }

    @PostMapping("/{catalogo}/rechazar-todos")
    public ResponseEntity<ApiResponse<Integer>> rechazarTodos(@PathVariable String catalogo) {
        requireAdmin();
        Long empresaId = requireEmpresaId();
        int n = service.rechazarTodos(empresaId, catalogo, currentUser.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.ok(n, "Desactivados " + n + " items globales"));
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
