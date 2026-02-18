package com.usco.parqueaderos_api.parking.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.parking.dto.ParqueaderoDTO;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.service.EmpresaService;
import com.usco.parqueaderos_api.parking.service.ParqueaderoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ParkingController {

    private final EmpresaService empresaService;
    private final ParqueaderoService parqueaderoService;

    // ---- Empresas ----
    @GetMapping("/api/empresas")
    public ResponseEntity<ApiResponse<List<Empresa>>> getAllEmpresas() {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.findAll()));
    }

    @GetMapping("/api/empresas/{id}")
    public ResponseEntity<ApiResponse<Empresa>> getEmpresaById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.findById(id)));
    }

    @PostMapping("/api/empresas")
    public ResponseEntity<ApiResponse<Empresa>> createEmpresa(@RequestBody Empresa empresa) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(empresaService.save(empresa)));
    }

    @PutMapping("/api/empresas/{id}")
    public ResponseEntity<ApiResponse<Empresa>> updateEmpresa(@PathVariable Long id, @RequestBody Empresa empresa) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.update(id, empresa)));
    }

    @DeleteMapping("/api/empresas/{id}")
    public ResponseEntity<Void> deleteEmpresa(@PathVariable Long id) {
        empresaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Parqueaderos ----
    @GetMapping("/api/parqueaderos")
    public ResponseEntity<ApiResponse<List<ParqueaderoDTO>>> getAllParqueaderos() {
        return ResponseEntity.ok(ApiResponse.ok(parqueaderoService.findAll()));
    }

    @GetMapping("/api/parqueaderos/{id}")
    public ResponseEntity<ApiResponse<ParqueaderoDTO>> getParqueaderoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(parqueaderoService.findById(id)));
    }

    @PostMapping("/api/parqueaderos")
    public ResponseEntity<ApiResponse<ParqueaderoDTO>> createParqueadero(@Valid @RequestBody ParqueaderoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(parqueaderoService.save(dto)));
    }

    @PutMapping("/api/parqueaderos/{id}")
    public ResponseEntity<ApiResponse<ParqueaderoDTO>> updateParqueadero(@PathVariable Long id, @Valid @RequestBody ParqueaderoDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(parqueaderoService.update(id, dto)));
    }

    @DeleteMapping("/api/parqueaderos/{id}")
    public ResponseEntity<Void> deleteParqueadero(@PathVariable Long id) {
        parqueaderoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
