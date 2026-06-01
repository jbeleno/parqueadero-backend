package com.usco.parqueaderos_api.catalog.global.controller;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.global.service.CatalogoAdminService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRUD admin de catalogos globales. v50 Sprint 3.
 *
 * Genericos: {catalogo} ∈
 *   tipos-documento, generos, monedas, zonas-horarias, unidades-tarifa,
 *   regimenes-tributarios, estados-civiles, paises-placa,
 *   tipos-servicio-vehiculo, tipos-acceso-dispositivo, canales-origen-reserva
 *
 * Reglas RBAC:
 *  - empresaId NULL en el item (global) → solo SUPER_ADMIN puede crear/editar/archivar.
 *  - empresaId NOT NULL (custom de empresa X) → ADMIN de X o SUPER_ADMIN.
 *
 * Estos endpoints estan bajo /api/admin/** y la SecurityConfig solo permite
 * ADMIN/SUPER_ADMIN entrar. Aqui aplicamos un check mas fino segun el item.
 */
@RestController
@RequestMapping("/api/admin/catalogos")
@RequiredArgsConstructor
public class AdminCatalogoController {

    private static final Set<String> CATALOGOS_VALIDOS = Set.of(
            "tipos-documento", "generos", "monedas", "zonas-horarias",
            "unidades-tarifa", "regimenes-tributarios", "estados-civiles",
            "paises-placa", "tipos-servicio-vehiculo", "tipos-acceso-dispositivo",
            "canales-origen-reserva"
    );

    private final CatalogoAdminService adminService;
    private final CurrentUserService currentUser;

    private void validarCatalogo(String catalogo) {
        if (!CATALOGOS_VALIDOS.contains(catalogo)) {
            throw new BusinessException(
                    "Catalogo desconocido: " + catalogo + ". Validos: " + CATALOGOS_VALIDOS,
                    "ERR_CATALOG_UNKNOWN");
        }
    }

    /**
     * Verifica que el usuario actual pueda tocar un item del catalogo segun la empresaId del item.
     */
    private void requirePermisoSobreItem(Long empresaIdItem) {
        boolean isSuperAdmin = currentUser.isSuperAdmin();
        if (empresaIdItem == null) {
            // Global → solo SUPER_ADMIN
            if (!isSuperAdmin) {
                throw new AccessDeniedException(
                        "Solo SUPER_ADMIN puede crear o modificar items canonicos globales");
            }
        } else {
            // Custom de empresa X → ADMIN de X o SUPER_ADMIN
            if (!isSuperAdmin) {
                currentUser.requireEmpresa(empresaIdItem);
                if (!currentUser.isAdmin()) {
                    throw new AccessDeniedException(
                            "Solo ADMIN/SUPER_ADMIN puede gestionar items custom");
                }
            }
        }
    }

    @GetMapping("/{catalogo}")
    public ResponseEntity<ApiResponse<List<?>>> listar(@PathVariable String catalogo) {
        validarCatalogo(catalogo);
        // SUPER_ADMIN ve todo; ADMIN solo globales + sus customs
        Long scope = currentUser.isSuperAdmin()
                ? null
                : currentUser.getCurrentEmpresaId().orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.listarTodos(catalogo, scope),
                "Items de " + catalogo));
    }

    @GetMapping("/{catalogo}/{id}")
    public ResponseEntity<ApiResponse<Object>> getById(
            @PathVariable String catalogo,
            @PathVariable Long id) {
        validarCatalogo(catalogo);
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getById(catalogo, id),
                catalogo + " #" + id));
    }

    @PostMapping("/{catalogo}")
    public ResponseEntity<ApiResponse<Object>> crear(
            @PathVariable String catalogo,
            @RequestBody Map<String, Object> body) {
        validarCatalogo(catalogo);
        // Validar permiso sobre el empresaId que viene en el body
        Long empresaIdItem = asLong(body.get("empresaId"));
        // Si no viene empresaId pero el caller es ADMIN (no SUPER_ADMIN), forzar a SU empresa
        if (empresaIdItem == null && !currentUser.isSuperAdmin()) {
            Long propio = currentUser.getCurrentEmpresaId().orElse(null);
            if (propio == null) {
                throw new AccessDeniedException("Usuario sin empresa asociada");
            }
            body.put("empresaId", propio);
            empresaIdItem = propio;
        }
        requirePermisoSobreItem(empresaIdItem);
        Object saved = adminService.crear(catalogo, body);
        return ResponseEntity.ok(ApiResponse.ok(saved, "Item creado"));
    }

    @PutMapping("/{catalogo}/{id}")
    public ResponseEntity<ApiResponse<Object>> actualizar(
            @PathVariable String catalogo,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        validarCatalogo(catalogo);
        Long empresaIdItem = adminService.getEmpresaIdDelItem(catalogo, id);
        requirePermisoSobreItem(empresaIdItem);
        Object updated = adminService.actualizar(catalogo, id, body);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Item actualizado"));
    }

    @PatchMapping("/{catalogo}/{id}/archivar")
    public ResponseEntity<ApiResponse<Object>> archivar(
            @PathVariable String catalogo,
            @PathVariable Long id) {
        validarCatalogo(catalogo);
        Long empresaIdItem = adminService.getEmpresaIdDelItem(catalogo, id);
        requirePermisoSobreItem(empresaIdItem);
        Object archived = adminService.archivar(catalogo, id);
        return ResponseEntity.ok(ApiResponse.ok(archived, "Item archivado"));
    }

    @PatchMapping("/{catalogo}/{id}/desarchivar")
    public ResponseEntity<ApiResponse<Object>> desarchivar(
            @PathVariable String catalogo,
            @PathVariable Long id) {
        validarCatalogo(catalogo);
        Long empresaIdItem = adminService.getEmpresaIdDelItem(catalogo, id);
        requirePermisoSobreItem(empresaIdItem);
        Object restored = adminService.desarchivar(catalogo, id);
        return ResponseEntity.ok(ApiResponse.ok(restored, "Item restaurado"));
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }
}
