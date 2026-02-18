package com.usco.parqueaderos_api.auth.controller;

import com.usco.parqueaderos_api.auth.dto.AsignarRolRequest;
import com.usco.parqueaderos_api.auth.dto.UsuarioAdminDTO;
import com.usco.parqueaderos_api.auth.service.AdminService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /** Listar todos los usuarios (paginado) */
    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<Page<UsuarioAdminDTO>>> listarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy) {
        Page<UsuarioAdminDTO> result = adminService.listarUsuarios(
                PageRequest.of(page, size, Sort.by(sortBy)));
        return ResponseEntity.ok(ApiResponse.ok(result, "Usuarios listados"));
    }

    /** Ver roles de un usuario */
    @GetMapping("/usuarios/{id}/roles")
    public ResponseEntity<ApiResponse<List<String>>> getRoles(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getRoles(id), "Roles del usuario"));
    }

    /** Asignar rol a usuario */
    @PostMapping("/usuarios/{id}/roles")
    public ResponseEntity<ApiResponse<String>> asignarRol(
            @PathVariable Long id,
            @Valid @RequestBody AsignarRolRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.asignarRol(id, request), "Rol asignado"));
    }

    /** Quitar rol a usuario */
    @DeleteMapping("/usuarios/{id}/roles/{rolId}")
    public ResponseEntity<ApiResponse<String>> quitarRol(
            @PathVariable Long id,
            @PathVariable Long rolId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.quitarRol(id, rolId), "Rol removido"));
    }

    /** Cambiar estado de un usuario */
    @PutMapping("/usuarios/{id}/estado")
    public ResponseEntity<ApiResponse<String>> cambiarEstado(
            @PathVariable Long id,
            @RequestParam Long estadoId) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.cambiarEstadoUsuario(id, estadoId), "Estado actualizado"));
    }
}
