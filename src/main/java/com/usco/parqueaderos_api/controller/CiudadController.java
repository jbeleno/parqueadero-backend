package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Ciudad;
import com.usco.parqueaderos_api.repository.CiudadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ciudades")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CiudadController {

    private final CiudadRepository ciudadRepository;

    @GetMapping
    public ResponseEntity<List<Ciudad>> getAll() {
        return ResponseEntity.ok(ciudadRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ciudad> getById(@PathVariable Long id) {
        return ciudadRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Ciudad> create(@RequestBody Ciudad ciudad) {
        return ResponseEntity.ok(ciudadRepository.save(ciudad));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ciudad> update(@PathVariable Long id, @RequestBody Ciudad ciudad) {
        if (!ciudadRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        ciudad.setId(id);
        return ResponseEntity.ok(ciudadRepository.save(ciudad));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!ciudadRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        ciudadRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
