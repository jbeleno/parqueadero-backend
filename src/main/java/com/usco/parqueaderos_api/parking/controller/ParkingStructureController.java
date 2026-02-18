package com.usco.parqueaderos_api.parking.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.parking.dto.NivelDTO;
import com.usco.parqueaderos_api.parking.dto.PuntoParqueoDTO;
import com.usco.parqueaderos_api.parking.dto.SeccionDTO;
import com.usco.parqueaderos_api.parking.dto.SubSeccionDTO;
import com.usco.parqueaderos_api.parking.service.NivelService;
import com.usco.parqueaderos_api.parking.service.PuntoParqueoService;
import com.usco.parqueaderos_api.parking.service.SeccionService;
import com.usco.parqueaderos_api.parking.service.SubSeccionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ParkingStructureController {

    private final NivelService nivelService;
    private final SeccionService seccionService;
    private final SubSeccionService subSeccionService;
    private final PuntoParqueoService puntoParqueoService;

    // ── Niveles ──────────────────────────────────────────────────────

    @GetMapping("/api/niveles")
    public ResponseEntity<ApiResponse<List<NivelDTO>>> getAllNiveles() {
        return ResponseEntity.ok(ApiResponse.ok(nivelService.findAll()));
    }

    @GetMapping("/api/niveles/{id}")
    public ResponseEntity<ApiResponse<NivelDTO>> getNivelById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(nivelService.findById(id)));
    }

    @GetMapping("/api/niveles/parqueadero/{parqueaderoId}")
    public ResponseEntity<ApiResponse<List<NivelDTO>>> getNivelesByParqueadero(@PathVariable Long parqueaderoId) {
        return ResponseEntity.ok(ApiResponse.ok(nivelService.findByParqueadero(parqueaderoId)));
    }

    @PostMapping("/api/niveles")
    public ResponseEntity<ApiResponse<NivelDTO>> createNivel(@Valid @RequestBody NivelDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(nivelService.save(dto)));
    }

    @PutMapping("/api/niveles/{id}")
    public ResponseEntity<ApiResponse<NivelDTO>> updateNivel(@PathVariable Long id, @Valid @RequestBody NivelDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(nivelService.update(id, dto)));
    }

    @PatchMapping("/api/niveles/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> archivarNivel(@PathVariable Long id) {
        nivelService.archivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Nivel archivado correctamente"));
    }

    // ── Secciones ────────────────────────────────────────────────────

    @GetMapping("/api/secciones")
    public ResponseEntity<ApiResponse<List<SeccionDTO>>> getAllSecciones() {
        return ResponseEntity.ok(ApiResponse.ok(seccionService.findAll()));
    }

    @GetMapping("/api/secciones/{id}")
    public ResponseEntity<ApiResponse<SeccionDTO>> getSeccionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(seccionService.findById(id)));
    }

    @PostMapping("/api/secciones")
    public ResponseEntity<ApiResponse<SeccionDTO>> createSeccion(@Valid @RequestBody SeccionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(seccionService.save(dto)));
    }

    @PutMapping("/api/secciones/{id}")
    public ResponseEntity<ApiResponse<SeccionDTO>> updateSeccion(@PathVariable Long id, @Valid @RequestBody SeccionDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(seccionService.update(id, dto)));
    }

    @PatchMapping("/api/secciones/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> archivarSeccion(@PathVariable Long id) {
        seccionService.archivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Sección archivada correctamente"));
    }

    // ── SubSecciones ─────────────────────────────────────────────────

    @GetMapping("/api/sub-secciones")
    public ResponseEntity<ApiResponse<List<SubSeccionDTO>>> getAllSubSecciones() {
        return ResponseEntity.ok(ApiResponse.ok(subSeccionService.findAll()));
    }

    @GetMapping("/api/sub-secciones/{id}")
    public ResponseEntity<ApiResponse<SubSeccionDTO>> getSubSeccionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(subSeccionService.findById(id)));
    }

    @PostMapping("/api/sub-secciones")
    public ResponseEntity<ApiResponse<SubSeccionDTO>> createSubSeccion(@Valid @RequestBody SubSeccionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(subSeccionService.save(dto)));
    }

    @PutMapping("/api/sub-secciones/{id}")
    public ResponseEntity<ApiResponse<SubSeccionDTO>> updateSubSeccion(@PathVariable Long id, @Valid @RequestBody SubSeccionDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(subSeccionService.update(id, dto)));
    }

    @PatchMapping("/api/sub-secciones/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> archivarSubSeccion(@PathVariable Long id) {
        subSeccionService.archivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Subsección archivada correctamente"));
    }

    // ── Puntos de Parqueo ────────────────────────────────────────────

    @GetMapping("/api/puntos-parqueo")
    public ResponseEntity<ApiResponse<List<PuntoParqueoDTO>>> getAllPuntosParqueo() {
        return ResponseEntity.ok(ApiResponse.ok(puntoParqueoService.findAll()));
    }

    @GetMapping("/api/puntos-parqueo/{id}")
    public ResponseEntity<ApiResponse<PuntoParqueoDTO>> getPuntoParqueoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(puntoParqueoService.findById(id)));
    }

    @PostMapping("/api/puntos-parqueo")
    public ResponseEntity<ApiResponse<PuntoParqueoDTO>> createPuntoParqueo(@Valid @RequestBody PuntoParqueoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(puntoParqueoService.save(dto)));
    }

    @PutMapping("/api/puntos-parqueo/{id}")
    public ResponseEntity<ApiResponse<PuntoParqueoDTO>> updatePuntoParqueo(@PathVariable Long id, @Valid @RequestBody PuntoParqueoDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(puntoParqueoService.update(id, dto)));
    }

    @PatchMapping("/api/puntos-parqueo/{id}/archivar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> archivarPuntoParqueo(@PathVariable Long id) {
        puntoParqueoService.archivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Punto de parqueo archivado correctamente"));
    }
}
