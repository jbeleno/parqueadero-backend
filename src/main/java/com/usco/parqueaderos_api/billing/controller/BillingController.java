package com.usco.parqueaderos_api.billing.controller;

import com.usco.parqueaderos_api.billing.dto.FacturaDTO;
import com.usco.parqueaderos_api.billing.dto.PagoDTO;
import com.usco.parqueaderos_api.billing.service.FacturaService;
import com.usco.parqueaderos_api.billing.service.PagoService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BillingController {

    private final FacturaService facturaService;
    private final PagoService pagoService;

    // ---- Facturas ----
    @GetMapping("/api/facturas")
    public ResponseEntity<ApiResponse<List<FacturaDTO>>> getAllFacturas() {
        return ResponseEntity.ok(ApiResponse.ok(facturaService.findAll()));
    }

    @GetMapping("/api/facturas/{id}")
    public ResponseEntity<ApiResponse<FacturaDTO>> getFacturaById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(facturaService.findById(id)));
    }

    @PostMapping("/api/facturas")
    public ResponseEntity<ApiResponse<FacturaDTO>> createFactura(@Valid @RequestBody FacturaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(facturaService.save(dto)));
    }

    @PutMapping("/api/facturas/{id}")
    public ResponseEntity<ApiResponse<FacturaDTO>> updateFactura(@PathVariable Long id, @Valid @RequestBody FacturaDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(facturaService.update(id, dto)));
    }

    @DeleteMapping("/api/facturas/{id}")
    public ResponseEntity<Void> deleteFactura(@PathVariable Long id) {
        facturaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Pagos ----
    @GetMapping("/api/pagos")
    public ResponseEntity<ApiResponse<List<PagoDTO>>> getAllPagos() {
        return ResponseEntity.ok(ApiResponse.ok(pagoService.findAll()));
    }

    @GetMapping("/api/pagos/{id}")
    public ResponseEntity<ApiResponse<PagoDTO>> getPagoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pagoService.findById(id)));
    }

    @PostMapping("/api/pagos")
    public ResponseEntity<ApiResponse<PagoDTO>> createPago(@Valid @RequestBody PagoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(pagoService.save(dto)));
    }

    @PutMapping("/api/pagos/{id}")
    public ResponseEntity<ApiResponse<PagoDTO>> updatePago(@PathVariable Long id, @Valid @RequestBody PagoDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(pagoService.update(id, dto)));
    }

    @DeleteMapping("/api/pagos/{id}")
    public ResponseEntity<Void> deletePago(@PathVariable Long id) {
        pagoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
