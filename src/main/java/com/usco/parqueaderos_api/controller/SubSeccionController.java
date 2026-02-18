package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.SubSeccion;
import com.usco.parqueaderos_api.repository.SubSeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sub-secciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SubSeccionController {

    private final SubSeccionRepository subSeccionRepository;

    @GetMapping
    public ResponseEntity<List<SubSeccion>> getAll() {
        return ResponseEntity.ok(subSeccionRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubSeccion> getById(@PathVariable Long id) {
        return subSeccionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SubSeccion> create(@RequestBody SubSeccion subSeccion) {
        return ResponseEntity.ok(subSeccionRepository.save(subSeccion));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubSeccion> update(@PathVariable Long id, @RequestBody SubSeccion subSeccion) {
        if (!subSeccionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        subSeccion.setId(id);
        return ResponseEntity.ok(subSeccionRepository.save(subSeccion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!subSeccionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        subSeccionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
