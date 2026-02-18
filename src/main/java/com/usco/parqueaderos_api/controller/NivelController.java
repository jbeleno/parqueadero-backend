package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Nivel;
import com.usco.parqueaderos_api.repository.NivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/niveles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NivelController {
    
    private final NivelRepository nivelRepository;
    
    @GetMapping
    public ResponseEntity<List<Nivel>> getAll() {
        return ResponseEntity.ok(nivelRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Nivel> getById(@PathVariable Long id) {
        return nivelRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Nivel> create(@RequestBody Nivel nivel) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(nivelRepository.save(nivel));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Nivel> update(@PathVariable Long id, @RequestBody Nivel nivel) {
        return nivelRepository.findById(id)
                .map(existing -> {
                    existing.setNombre(nivel.getNombre());
                    return ResponseEntity.ok(nivelRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        nivelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
