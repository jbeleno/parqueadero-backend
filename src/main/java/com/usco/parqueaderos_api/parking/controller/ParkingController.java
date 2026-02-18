package com.usco.parqueaderos_api.parking.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.parking.dto.EmpresaDTO;
import com.usco.parqueaderos_api.parking.dto.ParqueaderoDTO;
import com.usco.parqueaderos_api.parking.dto.config.ParkingLotConfigDTO;
import com.usco.parqueaderos_api.parking.service.EmpresaService;
import com.usco.parqueaderos_api.parking.service.ParkingConfigService;
import com.usco.parqueaderos_api.parking.service.ParqueaderoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ParkingController {

    private final EmpresaService empresaService;
    private final ParqueaderoService parqueaderoService;
    private final ParkingConfigService parkingConfigService;

    // ════════════════════════════════════════════════════════════════
    //  Empresas
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/api/empresas")
    public ResponseEntity<ApiResponse<List<EmpresaDTO>>> getAllEmpresas() {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.findAll()));
    }

    @GetMapping("/api/empresas/{id}")
    public ResponseEntity<ApiResponse<EmpresaDTO>> getEmpresaById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.findById(id)));
    }

    @PostMapping("/api/empresas")
    public ResponseEntity<ApiResponse<EmpresaDTO>> createEmpresa(@Valid @RequestBody EmpresaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(empresaService.save(dto)));
    }

    @PutMapping("/api/empresas/{id}")
    public ResponseEntity<ApiResponse<EmpresaDTO>> updateEmpresa(@PathVariable Long id, @Valid @RequestBody EmpresaDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(empresaService.update(id, dto)));
    }

    /** Soft-delete: archiva la empresa (solo ADMIN/SUPER_ADMIN) */
    @PatchMapping("/api/empresas/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> archivarEmpresa(@PathVariable Long id) {
        empresaService.archivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Empresa archivada correctamente"));
    }

    // ════════════════════════════════════════════════════════════════
    //  Parqueaderos (CRUD básico)
    // ════════════════════════════════════════════════════════════════

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

    /** Soft-delete: archiva el parqueadero con toda su configuración (solo ADMIN/SUPER_ADMIN) */
    @PatchMapping("/api/parqueaderos/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> archivarParqueadero(@PathVariable Long id) {
        parkingConfigService.archivarParqueadero(id);
        return ResponseEntity.ok(ApiResponse.ok("Parqueadero y toda su configuración archivados correctamente"));
    }

    // ════════════════════════════════════════════════════════════════
    //  Configuración completa (diseño del parqueadero)
    // ════════════════════════════════════════════════════════════════

    /** Guarda toda la configuración del parqueadero (pisos, secciones, subsecciones, puntos, caminos) */
    @PostMapping("/api/parqueaderos/configuracion")
    public ResponseEntity<ApiResponse<ParkingLotConfigDTO>> saveConfig(
            @RequestBody ParkingLotConfigDTO configDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(parkingConfigService.saveConfig(configDTO)));
    }

    /** Actualiza la configuración existente de un parqueadero */
    @PutMapping("/api/parqueaderos/{id}/configuracion")
    public ResponseEntity<ApiResponse<ParkingLotConfigDTO>> updateConfig(
            @PathVariable Long id,
            @RequestBody ParkingLotConfigDTO configDTO) {
        // Asegurar que el id del path coincida con el DTO
        if (configDTO.getParkingLot() != null) {
            configDTO.getParkingLot().setId(id);
        }
        return ResponseEntity.ok(ApiResponse.ok(parkingConfigService.saveConfig(configDTO)));
    }

    /** Retorna la configuración completa del parqueadero para que el frontend lo dibuje */
    @GetMapping("/api/parqueaderos/{id}/configuracion")
    public ResponseEntity<ApiResponse<ParkingLotConfigDTO>> getConfig(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(parkingConfigService.getConfig(id)));
    }

    /** Archiva un nivel específico y todos sus hijos (solo ADMIN/SUPER_ADMIN) */
    @PatchMapping("/api/parqueaderos/niveles/{nivelId}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> archivarNivel(@PathVariable Long nivelId) {
        parkingConfigService.archivarNivel(nivelId);
        return ResponseEntity.ok(ApiResponse.ok("Nivel y su configuración archivados correctamente"));
    }
}

