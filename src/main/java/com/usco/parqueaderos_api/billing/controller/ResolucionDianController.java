package com.usco.parqueaderos_api.billing.controller;

import com.usco.parqueaderos_api.billing.dto.ResolucionDianDTO;
import com.usco.parqueaderos_api.billing.service.ResolucionDianService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion de resoluciones DIAN por parqueadero. Cada parqueadero puede tener
 * varias registradas; el admin marca cual es la "principal" (default para
 * emitir facturas). Solo UNA principal activa por parqueadero a la vez.
 *
 * RBAC: ADMIN_PARQUEADERO, ADMIN, SUPER_ADMIN. NO OPERARIO_CAJA
 * (es informacion fiscal sensible).
 */
@RestController
@RequestMapping("/api/resoluciones-dian")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO')")
public class ResolucionDianController {

    private final ResolucionDianService service;

    /** Todas las NO archivadas del parqueadero (para el SELECT del front). */
    @GetMapping("/parqueadero/{parqueaderoId}")
    public ResponseEntity<ApiResponse<List<ResolucionDianDTO>>> listar(@PathVariable Long parqueaderoId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listarPorParqueadero(parqueaderoId)));
    }

    /** La resolucion marcada como principal del parqueadero (la que usa AutoFacturaListener). */
    @GetMapping("/parqueadero/{parqueaderoId}/principal")
    public ResponseEntity<ApiResponse<ResolucionDianDTO>> principal(@PathVariable Long parqueaderoId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findPrincipal(parqueaderoId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResolucionDianDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResolucionDianDTO>> crear(@Valid @RequestBody ResolucionDianDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.crear(dto), "Resolucion DIAN registrada"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ResolucionDianDTO>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ResolucionDianDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.actualizar(id, dto)));
    }

    /** Marca esta resolucion como principal. Desactiva auto la anterior del mismo parqueadero. */
    @PatchMapping("/{id}/marcar-principal")
    public ResponseEntity<ApiResponse<ResolucionDianDTO>> marcarPrincipal(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.marcarPrincipal(id), "Resolucion marcada como principal"));
    }

    /** Soft-delete. Si era principal queda desactivada. Motivo obligatorio (min 10 chars). */
    @PatchMapping("/{id}/archivar")
    public ResponseEntity<ApiResponse<ResolucionDianDTO>> archivar(
            @PathVariable Long id, @RequestParam String motivo) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.archivar(id, motivo), "Resolucion archivada"));
    }
}
