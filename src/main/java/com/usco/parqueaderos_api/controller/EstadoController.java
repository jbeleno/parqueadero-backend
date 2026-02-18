package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Estado;
import com.usco.parqueaderos_api.repository.EstadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estados")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EstadoController {
    
    private final EstadoRepository estadoRepository;
    
    @GetMapping
    public ResponseEntity<List<Estado>> getAll() {
        return ResponseEntity.ok(estadoRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Estado> getById(@PathVariable Long id) {
        return estadoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Estado> create(@RequestBody Estado estado) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estadoRepository.save(estado));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Estado> update(@PathVariable Long id, @RequestBody Estado estado) {
        return estadoRepository.findById(id)
                .map(existing -> {
                    existing.setNombre(estado.getNombre());
                    existing.setDescripcion(estado.getDescripcion());
                    return ResponseEntity.ok(estadoRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        estadoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
