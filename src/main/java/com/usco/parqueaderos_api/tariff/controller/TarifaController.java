package com.usco.parqueaderos_api.tariff.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.tariff.dto.TarifaDTO;
import com.usco.parqueaderos_api.tariff.service.TarifaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
public class TarifaController {

    private final TarifaService tarifaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TarifaDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TarifaDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TarifaDTO>> create(@Valid @RequestBody TarifaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(tarifaService.save(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TarifaDTO>> update(@PathVariable Long id, @Valid @RequestBody TarifaDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tarifaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
