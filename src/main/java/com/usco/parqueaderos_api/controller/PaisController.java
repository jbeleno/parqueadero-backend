package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Pais;
import com.usco.parqueaderos_api.repository.PaisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paises")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaisController {

    private final PaisRepository paisRepository;

    @GetMapping
    public ResponseEntity<List<Pais>> getAll() {
        return ResponseEntity.ok(paisRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pais> getById(@PathVariable Long id) {
        return paisRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Pais> create(@RequestBody Pais pais) {
        return ResponseEntity.ok(paisRepository.save(pais));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pais> update(@PathVariable Long id, @RequestBody Pais pais) {
        if (!paisRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        pais.setId(id);
        return ResponseEntity.ok(paisRepository.save(pais));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!paisRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        paisRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
