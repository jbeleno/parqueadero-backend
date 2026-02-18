package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.PuntoParqueo;
import com.usco.parqueaderos_api.repository.PuntoParqueoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/puntos-parqueo")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PuntoParqueoController {

    private final PuntoParqueoRepository puntoParqueoRepository;

    @GetMapping
    public ResponseEntity<List<PuntoParqueo>> getAll() {
        return ResponseEntity.ok(puntoParqueoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PuntoParqueo> getById(@PathVariable Long id) {
        return puntoParqueoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PuntoParqueo> create(@RequestBody PuntoParqueo puntoParqueo) {
        return ResponseEntity.ok(puntoParqueoRepository.save(puntoParqueo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PuntoParqueo> update(@PathVariable Long id, @RequestBody PuntoParqueo puntoParqueo) {
        if (!puntoParqueoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        puntoParqueo.setId(id);
        return ResponseEntity.ok(puntoParqueoRepository.save(puntoParqueo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!puntoParqueoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        puntoParqueoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
