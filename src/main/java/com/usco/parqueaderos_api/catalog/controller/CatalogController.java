package com.usco.parqueaderos_api.catalog.controller;

import com.usco.parqueaderos_api.catalog.entity.*;
import com.usco.parqueaderos_api.catalog.service.CatalogService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    // ---- Estados ----
    @GetMapping("/api/estados")
    public ResponseEntity<ApiResponse<List<Estado>>> getAllEstados() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllEstados()));
    }

    @GetMapping("/api/estados/{id}")
    public ResponseEntity<ApiResponse<Estado>> getEstadoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findEstadoById(id)));
    }

    @PostMapping("/api/estados")
    public ResponseEntity<ApiResponse<Estado>> createEstado(@RequestBody Estado estado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveEstado(estado)));
    }

    @PutMapping("/api/estados/{id}")
    public ResponseEntity<ApiResponse<Estado>> updateEstado(@PathVariable Long id, @RequestBody Estado estado) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateEstado(id, estado)));
    }

    @DeleteMapping("/api/estados/{id}")
    public ResponseEntity<Void> deleteEstado(@PathVariable Long id) {
        catalogService.deleteEstado(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Tipos de Parqueadero ----
    @GetMapping("/api/tipos-parqueadero")
    public ResponseEntity<ApiResponse<List<TipoParqueadero>>> getAllTiposParqueadero() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllTiposParqueadero()));
    }

    @GetMapping("/api/tipos-parqueadero/{id}")
    public ResponseEntity<ApiResponse<TipoParqueadero>> getTipoParqueaderoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findTipoParqueaderoById(id)));
    }

    @PostMapping("/api/tipos-parqueadero")
    public ResponseEntity<ApiResponse<TipoParqueadero>> createTipoParqueadero(@RequestBody TipoParqueadero tp) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveTipoParqueadero(tp)));
    }

    @PutMapping("/api/tipos-parqueadero/{id}")
    public ResponseEntity<ApiResponse<TipoParqueadero>> updateTipoParqueadero(@PathVariable Long id, @RequestBody TipoParqueadero tp) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateTipoParqueadero(id, tp)));
    }

    @DeleteMapping("/api/tipos-parqueadero/{id}")
    public ResponseEntity<Void> deleteTipoParqueadero(@PathVariable Long id) {
        catalogService.deleteTipoParqueadero(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Tipos de Punto de Parqueo ----
    @GetMapping("/api/tipos-punto-parqueo")
    public ResponseEntity<ApiResponse<List<TipoPuntoParqueo>>> getAllTiposPuntoParqueo() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllTiposPuntoParqueo()));
    }

    @GetMapping("/api/tipos-punto-parqueo/{id}")
    public ResponseEntity<ApiResponse<TipoPuntoParqueo>> getTipoPuntoParqueoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findTipoPuntoParqueoById(id)));
    }

    @PostMapping("/api/tipos-punto-parqueo")
    public ResponseEntity<ApiResponse<TipoPuntoParqueo>> createTipoPuntoParqueo(@RequestBody TipoPuntoParqueo tp) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveTipoPuntoParqueo(tp)));
    }

    @PutMapping("/api/tipos-punto-parqueo/{id}")
    public ResponseEntity<ApiResponse<TipoPuntoParqueo>> updateTipoPuntoParqueo(@PathVariable Long id, @RequestBody TipoPuntoParqueo tp) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateTipoPuntoParqueo(id, tp)));
    }

    @DeleteMapping("/api/tipos-punto-parqueo/{id}")
    public ResponseEntity<Void> deleteTipoPuntoParqueo(@PathVariable Long id) {
        catalogService.deleteTipoPuntoParqueo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Tipos de Vehículo ----
    @GetMapping("/api/tipos-vehiculo")
    public ResponseEntity<ApiResponse<List<TipoVehiculo>>> getAllTiposVehiculo() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllTiposVehiculo()));
    }

    @GetMapping("/api/tipos-vehiculo/{id}")
    public ResponseEntity<ApiResponse<TipoVehiculo>> getTipoVehiculoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findTipoVehiculoById(id)));
    }

    @PostMapping("/api/tipos-vehiculo")
    public ResponseEntity<ApiResponse<TipoVehiculo>> createTipoVehiculo(@RequestBody TipoVehiculo tv) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveTipoVehiculo(tv)));
    }

    @PutMapping("/api/tipos-vehiculo/{id}")
    public ResponseEntity<ApiResponse<TipoVehiculo>> updateTipoVehiculo(@PathVariable Long id, @RequestBody TipoVehiculo tv) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateTipoVehiculo(id, tv)));
    }

    @DeleteMapping("/api/tipos-vehiculo/{id}")
    public ResponseEntity<Void> deleteTipoVehiculo(@PathVariable Long id) {
        catalogService.deleteTipoVehiculo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Tipos de Dispositivo ----
    @GetMapping("/api/tipos-dispositivo")
    public ResponseEntity<ApiResponse<List<TipoDispositivo>>> getAllTiposDispositivo() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllTiposDispositivo()));
    }

    @GetMapping("/api/tipos-dispositivo/{id}")
    public ResponseEntity<ApiResponse<TipoDispositivo>> getTipoDispositivoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findTipoDispositivoById(id)));
    }

    @PostMapping("/api/tipos-dispositivo")
    public ResponseEntity<ApiResponse<TipoDispositivo>> createTipoDispositivo(@RequestBody TipoDispositivo td) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveTipoDispositivo(td)));
    }

    @PutMapping("/api/tipos-dispositivo/{id}")
    public ResponseEntity<ApiResponse<TipoDispositivo>> updateTipoDispositivo(@PathVariable Long id, @RequestBody TipoDispositivo td) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateTipoDispositivo(id, td)));
    }

    @DeleteMapping("/api/tipos-dispositivo/{id}")
    public ResponseEntity<Void> deleteTipoDispositivo(@PathVariable Long id) {
        catalogService.deleteTipoDispositivo(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Roles ----
    @GetMapping("/api/roles")
    public ResponseEntity<ApiResponse<List<Rol>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findAllRoles()));
    }

    @GetMapping("/api/roles/{id}")
    public ResponseEntity<ApiResponse<Rol>> getRolById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.findRolById(id)));
    }

    @PostMapping("/api/roles")
    public ResponseEntity<ApiResponse<Rol>> createRol(@RequestBody Rol rol) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(catalogService.saveRol(rol)));
    }

    @PutMapping("/api/roles/{id}")
    public ResponseEntity<ApiResponse<Rol>> updateRol(@PathVariable Long id, @RequestBody Rol rol) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.updateRol(id, rol)));
    }

    @DeleteMapping("/api/roles/{id}")
    public ResponseEntity<Void> deleteRol(@PathVariable Long id) {
        catalogService.deleteRol(id);
        return ResponseEntity.noContent().build();
    }
}
