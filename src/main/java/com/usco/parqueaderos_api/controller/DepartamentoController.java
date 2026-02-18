package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Departamento;
import com.usco.parqueaderos_api.repository.DepartamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departamentos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DepartamentoController {

    private final DepartamentoRepository departamentoRepository;

    @GetMapping
    public ResponseEntity<List<Departamento>> getAll() {
        return ResponseEntity.ok(departamentoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Departamento> getById(@PathVariable Long id) {
        return departamentoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Departamento> create(@RequestBody Departamento departamento) {
        return ResponseEntity.ok(departamentoRepository.save(departamento));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Departamento> update(@PathVariable Long id, @RequestBody Departamento departamento) {
        if (!departamentoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        departamento.setId(id);
        return ResponseEntity.ok(departamentoRepository.save(departamento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!departamentoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        departamentoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
