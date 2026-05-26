package com.usco.parqueaderos_api.billing.controller;

import com.usco.parqueaderos_api.billing.dto.DeudaVehiculoDTO;
import com.usco.parqueaderos_api.billing.service.DeudaService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeudaController {

    private final DeudaService deudaService;

    /** Deuda de un vehiculo (todas sus facturas PENDIENTE). */
    @GetMapping("/vehiculos/{id}/deuda")
    public ResponseEntity<ApiResponse<DeudaVehiculoDTO>> deudaVehiculo(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(deudaService.deudaPorVehiculo(id)));
    }

    /** Lista de vehiculos morosos del operador, agrupada por vehiculo y ordenada por monto. */
    @GetMapping("/vehiculos/morosos")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<DeudaVehiculoDTO>>> morosos() {
        return ResponseEntity.ok(ApiResponse.ok(deudaService.listarMorosos()));
    }
}
