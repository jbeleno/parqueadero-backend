package com.usco.parqueaderos_api.vehicle.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.vehicle.dto.VehiculoDTO;
import com.usco.parqueaderos_api.vehicle.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService vehiculoService;

    /**
     * Lista vehiculos.
     * @param soloMiEmpresa si true, ADMIN ve solo vehiculos con actividad
     *                       (tickets/reservas) en parqueaderos de su empresa.
     *                       SUPER_ADMIN ignora el flag (siempre todos).
     *                       USER ignora el flag (siempre solo los suyos).
     *                       Default: false.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VehiculoDTO>>> getAll(
            @RequestParam(name = "soloMiEmpresa", required = false, defaultValue = "false") boolean soloMiEmpresa) {
        return ResponseEntity.ok(ApiResponse.ok(vehiculoService.findAll(soloMiEmpresa)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehiculoDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(vehiculoService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehiculoDTO>> create(@Valid @RequestBody VehiculoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(vehiculoService.save(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehiculoDTO>> update(@PathVariable Long id, @Valid @RequestBody VehiculoDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(vehiculoService.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vehiculoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
