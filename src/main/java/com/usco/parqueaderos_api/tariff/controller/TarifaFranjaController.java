package com.usco.parqueaderos_api.tariff.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.tariff.dto.TarifaFranjaDTO;
import com.usco.parqueaderos_api.tariff.service.TarifaFranjaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarifas/franjas")
@RequiredArgsConstructor
public class TarifaFranjaController {

    private final TarifaFranjaService service;

    @GetMapping("/por-tarifa/{tarifaId}")
    public ResponseEntity<ApiResponse<List<TarifaFranjaDTO>>> byTarifa(@PathVariable Long tarifaId) {
        return ResponseEntity.ok(ApiResponse.ok(service.findByTarifa(tarifaId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TarifaFranjaDTO>> create(@RequestBody TarifaFranjaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.save(dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
