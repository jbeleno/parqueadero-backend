package com.usco.parqueaderos_api.catalog.controller;

import com.usco.parqueaderos_api.catalog.dto.RolDTO;
import com.usco.parqueaderos_api.catalog.dto.TipoDispositivoDTO;
import com.usco.parqueaderos_api.catalog.dto.TipoParqueaderoDTO;
import com.usco.parqueaderos_api.catalog.dto.TipoPuntoParqueoDTO;
import com.usco.parqueaderos_api.catalog.entity.*;
import com.usco.parqueaderos_api.catalog.service.CatalogService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogos del sistema (estados, tipos, roles). Lectura abierta a autenticados.
 * Mutaciones solo SUPER_ADMIN: cambiar estos catalogos puede romper integridad
 * referencial multi-tenant.
 */
@RestController
@RequiredArgsConstructor
public class CatalogController {

    private static final String SUPER = "hasRole('SUPER_ADMIN')";

    private final CatalogService catalogService;

    // ---- Estados (sin lazy, devuelve entidad directo) ----
    @GetMapping("/api/estados")
    public ResponseEntity<ApiResponse<List<Estado>>> getAllEstados() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllEstados()));
    }

    @GetMapping("/api/estados/{id}")
    public ResponseEntity<ApiResponse<Estado>> getEstadoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findEstadoById(id)));
    }

    @PreAuthorize(SUPER)
    @PostMapping("/api/estados")
    public ResponseEntity<ApiResponse<Estado>> createEstado(@RequestBody Estado estado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveEstado(estado)));
    }

    @PreAuthorize(SUPER)
    @PutMapping("/api/estados/{id}")
    public ResponseEntity<ApiResponse<Estado>> updateEstado(@PathVariable Long id, @RequestBody Estado estado) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateEstado(id, estado)));
    }

    @PreAuthorize(SUPER)
    @DeleteMapping("/api/estados/{id}")
    public ResponseEntity<Void> deleteEstado(@PathVariable Long id) {
        catalogService.deleteEstado(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Tipos de Parqueadero ----
    @GetMapping("/api/tipos-parqueadero")
    public ResponseEntity<ApiResponse<List<TipoParqueaderoDTO>>> getAllTiposParqueadero() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllTiposParqueadero()));
    }

    @GetMapping("/api/tipos-parqueadero/{id}")
    public ResponseEntity<ApiResponse<TipoParqueaderoDTO>> getTipoParqueaderoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findTipoParqueaderoById(id)));
    }

    @PreAuthorize(SUPER)
    @PostMapping("/api/tipos-parqueadero")
    public ResponseEntity<ApiResponse<TipoParqueaderoDTO>> createTipoParqueadero(@RequestBody TipoParqueadero tp) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveTipoParqueadero(tp)));
    }

    @PreAuthorize(SUPER)
    @PutMapping("/api/tipos-parqueadero/{id}")
    public ResponseEntity<ApiResponse<TipoParqueaderoDTO>> updateTipoParqueadero(@PathVariable Long id, @RequestBody TipoParqueadero tp) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateTipoParqueadero(id, tp)));
    }

    @PreAuthorize(SUPER)
    @DeleteMapping("/api/tipos-parqueadero/{id}")
    public ResponseEntity<Void> deleteTipoParqueadero(@PathVariable Long id) {
        catalogService.deleteTipoParqueadero(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Tipos de Punto de Parqueo ----
    @GetMapping("/api/tipos-punto-parqueo")
    public ResponseEntity<ApiResponse<List<TipoPuntoParqueoDTO>>> getAllTiposPuntoParqueo() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllTiposPuntoParqueo()));
    }

    @GetMapping("/api/tipos-punto-parqueo/{id}")
    public ResponseEntity<ApiResponse<TipoPuntoParqueoDTO>> getTipoPuntoParqueoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findTipoPuntoParqueoById(id)));
    }

    @PreAuthorize(SUPER)
    @PostMapping("/api/tipos-punto-parqueo")
    public ResponseEntity<ApiResponse<TipoPuntoParqueoDTO>> createTipoPuntoParqueo(@RequestBody TipoPuntoParqueo tp) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveTipoPuntoParqueo(tp)));
    }

    @PreAuthorize(SUPER)
    @PutMapping("/api/tipos-punto-parqueo/{id}")
    public ResponseEntity<ApiResponse<TipoPuntoParqueoDTO>> updateTipoPuntoParqueo(@PathVariable Long id, @RequestBody TipoPuntoParqueo tp) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateTipoPuntoParqueo(id, tp)));
    }

    @PreAuthorize(SUPER)
    @DeleteMapping("/api/tipos-punto-parqueo/{id}")
    public ResponseEntity<Void> deleteTipoPuntoParqueo(@PathVariable Long id) {
        catalogService.deleteTipoPuntoParqueo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Tipos de Vehículo (sin lazy, devuelve entidad directo) ----
    @GetMapping("/api/tipos-vehiculo")
    public ResponseEntity<ApiResponse<List<TipoVehiculo>>> getAllTiposVehiculo() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllTiposVehiculo()));
    }

    @GetMapping("/api/tipos-vehiculo/{id}")
    public ResponseEntity<ApiResponse<TipoVehiculo>> getTipoVehiculoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findTipoVehiculoById(id)));
    }

    @PreAuthorize(SUPER)
    @PostMapping("/api/tipos-vehiculo")
    public ResponseEntity<ApiResponse<TipoVehiculo>> createTipoVehiculo(@RequestBody TipoVehiculo tv) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveTipoVehiculo(tv)));
    }

    @PreAuthorize(SUPER)
    @PutMapping("/api/tipos-vehiculo/{id}")
    public ResponseEntity<ApiResponse<TipoVehiculo>> updateTipoVehiculo(@PathVariable Long id, @RequestBody TipoVehiculo tv) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateTipoVehiculo(id, tv)));
    }

    @PreAuthorize(SUPER)
    @DeleteMapping("/api/tipos-vehiculo/{id}")
    public ResponseEntity<Void> deleteTipoVehiculo(@PathVariable Long id) {
        catalogService.deleteTipoVehiculo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Tipos de Dispositivo ----
    @GetMapping("/api/tipos-dispositivo")
    public ResponseEntity<ApiResponse<List<TipoDispositivoDTO>>> getAllTiposDispositivo() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllTiposDispositivo()));
    }

    @GetMapping("/api/tipos-dispositivo/{id}")
    public ResponseEntity<ApiResponse<TipoDispositivoDTO>> getTipoDispositivoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findTipoDispositivoById(id)));
    }

    @PreAuthorize(SUPER)
    @PostMapping("/api/tipos-dispositivo")
    public ResponseEntity<ApiResponse<TipoDispositivoDTO>> createTipoDispositivo(@RequestBody TipoDispositivo td) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveTipoDispositivo(td)));
    }

    @PreAuthorize(SUPER)
    @PutMapping("/api/tipos-dispositivo/{id}")
    public ResponseEntity<ApiResponse<TipoDispositivoDTO>> updateTipoDispositivo(@PathVariable Long id, @RequestBody TipoDispositivo td) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateTipoDispositivo(id, td)));
    }

    @PreAuthorize(SUPER)
    @DeleteMapping("/api/tipos-dispositivo/{id}")
    public ResponseEntity<Void> deleteTipoDispositivo(@PathVariable Long id) {
        catalogService.deleteTipoDispositivo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Roles ----
    @GetMapping("/api/roles")
    public ResponseEntity<ApiResponse<List<RolDTO>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllRoles()));
    }

    @GetMapping("/api/roles/{id}")
    public ResponseEntity<ApiResponse<RolDTO>> getRolById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findRolById(id)));
    }

    @PreAuthorize(SUPER)
    @PostMapping("/api/roles")
    public ResponseEntity<ApiResponse<RolDTO>> createRol(@RequestBody Rol rol) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveRol(rol)));
    }

    @PreAuthorize(SUPER)
    @PutMapping("/api/roles/{id}")
    public ResponseEntity<ApiResponse<RolDTO>> updateRol(@PathVariable Long id, @RequestBody Rol rol) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateRol(id, rol)));
    }

    @PreAuthorize(SUPER)
    @DeleteMapping("/api/roles/{id}")
    public ResponseEntity<Void> deleteRol(@PathVariable Long id) {
        catalogService.deleteRol(id);
        return ResponseEntity.noContent().build();
    }
}
