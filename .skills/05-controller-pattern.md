# Skill 05: Patrón de Controller

## Regla
Todos los controllers inyectan Service, NUNCA Repository. Usan `@Valid`, devuelven `ApiResponse<T>`, y usan códigos HTTP correctos.

---

## Patrón completo: Controller con DTO

```java
package com.usco.parqueaderos_api.parking.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.parking.dto.ParqueaderoDTO;
import com.usco.parqueaderos_api.parking.service.ParqueaderoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parqueaderos")
@RequiredArgsConstructor
public class ParqueaderoController {
    
    private final ParqueaderoService parqueaderoService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ParqueaderoDTO>>> getAll() {
        List<ParqueaderoDTO> data = parqueaderoService.findAll();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ParqueaderoDTO>> getById(@PathVariable Long id) {
        ParqueaderoDTO data = parqueaderoService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<ParqueaderoDTO>> create(@Valid @RequestBody ParqueaderoDTO dto) {
        ParqueaderoDTO created = parqueaderoService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ParqueaderoDTO>> update(
            @PathVariable Long id, 
            @Valid @RequestBody ParqueaderoDTO dto) {
        ParqueaderoDTO updated = parqueaderoService.update(id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parqueaderoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Patrón simple: Controller para catálogos (sin DTO)

```java
package com.usco.parqueaderos_api.catalog.controller;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.service.CatalogService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {
    
    private final CatalogService catalogService;
    
    // ===== ESTADOS =====
    
    @GetMapping("/estados")
    public ResponseEntity<ApiResponse<List<Estado>>> getAllEstados() {
        return ResponseEntity.ok(ApiResponse.success(catalogService.findAllEstados()));
    }
    
    @GetMapping("/estados/{id}")
    public ResponseEntity<ApiResponse<Estado>> getEstadoById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.findEstadoById(id)));
    }
    
    @PostMapping("/estados")
    public ResponseEntity<ApiResponse<Estado>> createEstado(@Valid @RequestBody Estado estado) {
        Estado created = catalogService.createEstado(estado);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created));
    }
    
    @PutMapping("/estados/{id}")
    public ResponseEntity<ApiResponse<Estado>> updateEstado(
            @PathVariable Long id, 
            @Valid @RequestBody Estado estado) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.updateEstado(id, estado)));
    }
    
    @DeleteMapping("/estados/{id}")
    public ResponseEntity<Void> deleteEstado(@PathVariable Long id) {
        catalogService.deleteEstado(id);
        return ResponseEntity.noContent().build();
    }
    
    // ===== TIPOS PARQUEADERO =====
    // Repetir el mismo patrón con /tipos-parqueadero
    
    // ===== TIPOS VEHICULO =====
    // Repetir el mismo patrón con /tipos-vehiculo
    
    // ===== TIPOS PUNTO PARQUEO =====
    // Repetir el mismo patrón con /tipos-punto-parqueo
    
    // ===== TIPOS DISPOSITIVO =====
    // Repetir el mismo patrón con /tipos-dispositivo
    
    // ===== ROLES =====
    // Repetir el mismo patrón con /roles
}
```

---

## Patrón: LocationController (agrupa Pais, Departamento, Ciudad)

```java
package com.usco.parqueaderos_api.location.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.location.entity.*;
import com.usco.parqueaderos_api.location.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LocationController {
    
    private final LocationService locationService;
    
    // ===== PAISES =====
    
    @GetMapping("/paises")
    public ResponseEntity<ApiResponse<List<Pais>>> getAllPaises() {
        return ResponseEntity.ok(ApiResponse.success(locationService.findAllPaises()));
    }
    
    @GetMapping("/paises/{id}")
    public ResponseEntity<ApiResponse<Pais>> getPaisById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(locationService.findPaisById(id)));
    }
    
    @PostMapping("/paises")
    public ResponseEntity<ApiResponse<Pais>> createPais(@Valid @RequestBody Pais pais) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(locationService.createPais(pais)));
    }
    
    @PutMapping("/paises/{id}")
    public ResponseEntity<ApiResponse<Pais>> updatePais(@PathVariable Long id, @Valid @RequestBody Pais pais) {
        return ResponseEntity.ok(ApiResponse.success(locationService.updatePais(id, pais)));
    }
    
    @DeleteMapping("/paises/{id}")
    public ResponseEntity<Void> deletePais(@PathVariable Long id) {
        locationService.deletePais(id);
        return ResponseEntity.noContent().build();
    }
    
    // ===== DEPARTAMENTOS =====
    // Mismo patrón con /departamentos
    
    // ===== CIUDADES =====
    // Mismo patrón con /ciudades
}
```

---

## HealthController (se queda igual, solo quitar @CrossOrigin)

```java
package com.usco.parqueaderos_api.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "message", "API de Parqueaderos funcionando correctamente"
        ));
    }
}
```

---

## Reglas inquebrantables para Controllers

1. **NUNCA** inyectar `Repository` en un controller
2. **SIEMPRE** usar `@Valid` antes de `@RequestBody`
3. **SIEMPRE** devolver `ApiResponse<T>` (excepto DELETE que devuelve `Void`)
4. **SIEMPRE** usar `HttpStatus.CREATED` (201) en POST
5. **SIEMPRE** usar `HttpStatus.NO_CONTENT` (204) en DELETE
6. **NO** poner `@CrossOrigin` en controllers individuales — se configura en `CorsConfig.java`
7. **SIEMPRE** `@RequiredArgsConstructor` (inyección por constructor vía Lombok)
8. El controller NO contiene lógica de negocio — solo delega al service

---

## Mapeo de URLs (mantener las mismas que ya existen)

| Recurso | URL | Controller |
|---------|-----|-----------|
| Health | `/api/health` | `HealthController` (common) |
| Estados | `/api/estados` | `CatalogController` |
| Tipos Parqueadero | `/api/tipos-parqueadero` | `CatalogController` |
| Tipos Punto Parqueo | `/api/tipos-punto-parqueo` | `CatalogController` |
| Tipos Vehículo | `/api/tipos-vehiculo` | `CatalogController` |
| Tipos Dispositivo | `/api/tipos-dispositivo` | `CatalogController` |
| Roles | `/api/roles` | `CatalogController` |
| Países | `/api/paises` | `LocationController` |
| Departamentos | `/api/departamentos` | `LocationController` |
| Ciudades | `/api/ciudades` | `LocationController` |
| Empresas | `/api/empresas` | `EmpresaController` (parking) |
| Parqueaderos | `/api/parqueaderos` | `ParqueaderoController` (parking) |
| Niveles | `/api/niveles` | `NivelController` (parking) |
| Secciones | `/api/secciones` | `SeccionController` (parking) |
| Sub-secciones | `/api/sub-secciones` | `SubSeccionController` (parking) |
| Puntos Parqueo | `/api/puntos-parqueo` | `PuntoParqueoController` (parking) |
| Personas | `/api/personas` | `PersonaController` (user) |
| Usuarios | `/api/usuarios` | `UsuarioController` (user) |
| Vehículos | `/api/vehiculos` | `VehiculoController` (vehicle) |
| Tickets | `/api/tickets` | `TicketController` (ticket) |
| Reservas | `/api/reservas` | `ReservaController` (reservation) |
| Tarifas | `/api/tarifas` | `TarifaController` (tariff) |
| Facturas | `/api/facturas` | `FacturaController` (billing) |
| Pagos | `/api/pagos` | `PagoController` (billing) |
| Dispositivos | `/api/dispositivos` | `DispositivoController` (device) |
| Auth | `/api/auth/login`, `/api/auth/register` | `AuthController` (auth) |
