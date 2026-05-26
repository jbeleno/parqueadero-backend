package com.usco.parqueaderos_api.parking.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.parking.dto.ConfiguracionReciboDTO;
import com.usco.parqueaderos_api.parking.service.ConfiguracionReciboService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parqueaderos")
@RequiredArgsConstructor
public class ConfiguracionReciboController {

    private final ConfiguracionReciboService service;

    /** Lectura: cualquier rol autenticado puede ver la config del recibo. */
    @GetMapping("/{id}/configuracion-recibo")
    public ResponseEntity<ApiResponse<ConfiguracionReciboDTO>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.obtener(id)));
    }

    /**
     * Edicion auditada. Requiere header X-Motivo-Operacion (interceptor global).
     * Roles permitidos: ADMIN, ADMIN_PARQUEADERO, SUPER_ADMIN.
     */
    @PatchMapping("/{id}/configuracion-recibo")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_PARQUEADERO','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ConfiguracionReciboDTO>> actualizar(
            @PathVariable Long id,
            @RequestBody ConfiguracionReciboDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(service.actualizar(id, dto)));
    }
}
