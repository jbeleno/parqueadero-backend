package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Seccion;
import com.usco.parqueaderos_api.repository.SeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/secciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SeccionController {

    private final SeccionRepository seccionRepository;

    @GetMapping
    public ResponseEntity<List<Seccion>> getAll() {
        return ResponseEntity.ok(seccionRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Seccion> getById(@PathVariable Long id) {
        return seccionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Seccion> create(@RequestBody Seccion seccion) {
        return ResponseEntity.ok(seccionRepository.save(seccion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Seccion> update(@PathVariable Long id, @RequestBody Seccion seccion) {
        if (!seccionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        seccion.setId(id);
        return ResponseEntity.ok(seccionRepository.save(seccion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!seccionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        seccionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
