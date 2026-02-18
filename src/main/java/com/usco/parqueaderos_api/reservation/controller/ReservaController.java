package com.usco.parqueaderos_api.reservation.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.reservation.dto.ReservaDTO;
import com.usco.parqueaderos_api.reservation.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservaDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(reservaService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservaDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reservaService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservaDTO>> create(@Valid @RequestBody ReservaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reservaService.save(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservaDTO>> update(@PathVariable Long id, @Valid @RequestBody ReservaDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(reservaService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
