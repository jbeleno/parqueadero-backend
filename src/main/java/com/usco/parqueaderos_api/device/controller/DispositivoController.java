package com.usco.parqueaderos_api.device.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.device.dto.DispositivoDTO;
import com.usco.parqueaderos_api.device.service.DispositivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispositivos")
@RequiredArgsConstructor
public class DispositivoController {

    private final DispositivoService dispositivoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DispositivoDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(dispositivoService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DispositivoDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(dispositivoService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DispositivoDTO>> create(@Valid @RequestBody DispositivoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dispositivoService.save(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DispositivoDTO>> update(@PathVariable Long id, @Valid @RequestBody DispositivoDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(dispositivoService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dispositivoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
