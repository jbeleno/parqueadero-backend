package com.usco.parqueaderos_api.location.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.location.entity.Ciudad;
import com.usco.parqueaderos_api.location.entity.Departamento;
import com.usco.parqueaderos_api.location.entity.Pais;
import com.usco.parqueaderos_api.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // ---- Países ----
    @GetMapping("/api/paises")
    public ResponseEntity<ApiResponse<List<Pais>>> getAllPaises() {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findAllPaises()));
    }

    @GetMapping("/api/paises/{id}")
    public ResponseEntity<ApiResponse<Pais>> getPaisById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findPaisById(id)));
    }

    @PostMapping("/api/paises")
    public ResponseEntity<ApiResponse<Pais>> createPais(@RequestBody Pais pais) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(locationService.savePais(pais)));
    }

    @PutMapping("/api/paises/{id}")
    public ResponseEntity<ApiResponse<Pais>> updatePais(@PathVariable Long id, @RequestBody Pais pais) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.updatePais(id, pais)));
    }

    @DeleteMapping("/api/paises/{id}")
    public ResponseEntity<Void> deletePais(@PathVariable Long id) {
        locationService.deletePais(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Departamentos ----
    @GetMapping("/api/departamentos")
    public ResponseEntity<ApiResponse<List<Departamento>>> getAllDepartamentos() {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findAllDepartamentos()));
    }

    @GetMapping("/api/departamentos/{id}")
    public ResponseEntity<ApiResponse<Departamento>> getDepartamentoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findDepartamentoById(id)));
    }

    @GetMapping("/api/departamentos/pais/{paisId}")
    public ResponseEntity<ApiResponse<List<Departamento>>> getDepartamentosByPais(@PathVariable Long paisId) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findDepartamentosByPaisId(paisId)));
    }

    @PostMapping("/api/departamentos")
    public ResponseEntity<ApiResponse<Departamento>> createDepartamento(@RequestBody Departamento departamento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(locationService.saveDepartamento(departamento)));
    }

    @PutMapping("/api/departamentos/{id}")
    public ResponseEntity<ApiResponse<Departamento>> updateDepartamento(@PathVariable Long id, @RequestBody Departamento departamento) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.updateDepartamento(id, departamento)));
    }

    @DeleteMapping("/api/departamentos/{id}")
    public ResponseEntity<Void> deleteDepartamento(@PathVariable Long id) {
        locationService.deleteDepartamento(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Ciudades ----
    @GetMapping("/api/ciudades")
    public ResponseEntity<ApiResponse<List<Ciudad>>> getAllCiudades() {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findAllCiudades()));
    }

    @GetMapping("/api/ciudades/{id}")
    public ResponseEntity<ApiResponse<Ciudad>> getCiudadById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findCiudadById(id)));
    }

    @GetMapping("/api/ciudades/departamento/{departamentoId}")
    public ResponseEntity<ApiResponse<List<Ciudad>>> getCiudadesByDepartamento(@PathVariable Long departamentoId) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findCiudadesByDepartamentoId(departamentoId)));
    }

    @PostMapping("/api/ciudades")
    public ResponseEntity<ApiResponse<Ciudad>> createCiudad(@RequestBody Ciudad ciudad) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(locationService.saveCiudad(ciudad)));
    }

    @PutMapping("/api/ciudades/{id}")
    public ResponseEntity<ApiResponse<Ciudad>> updateCiudad(@PathVariable Long id, @RequestBody Ciudad ciudad) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.updateCiudad(id, ciudad)));
    }

    @DeleteMapping("/api/ciudades/{id}")
    public ResponseEntity<Void> deleteCiudad(@PathVariable Long id) {
        locationService.deleteCiudad(id);
        return ResponseEntity.noContent().build();
    }
}
