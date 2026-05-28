package com.usco.parqueaderos_api.tariff.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.tariff.dto.TarifaDTO;
import com.usco.parqueaderos_api.tariff.service.TarifaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
public class TarifaController {

    private final TarifaService tarifaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TarifaDTO>>> getAll(
            @RequestParam(defaultValue = "false") boolean incluirArchivadas) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.findAll(incluirArchivadas)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TarifaDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO')")
    public ResponseEntity<ApiResponse<TarifaDTO>> create(@Valid @RequestBody TarifaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(tarifaService.save(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO')")
    public ResponseEntity<ApiResponse<TarifaDTO>> update(@PathVariable Long id, @Valid @RequestBody TarifaDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tarifaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Soft-delete con motivo. Permitido a ADMIN_PARQUEADERO + ADMIN + SUPER_ADMIN. */
    @PatchMapping("/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO')")
    public ResponseEntity<ApiResponse<TarifaDTO>> archivar(@PathVariable Long id,
                                                           @RequestParam String motivo) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.archivar(id, motivo)));
    }

    @PatchMapping("/{id}/desarchivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO')")
    public ResponseEntity<ApiResponse<TarifaDTO>> desarchivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.desarchivar(id)));
    }
}
