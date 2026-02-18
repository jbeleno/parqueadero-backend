package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.TipoParqueadero;
import com.usco.parqueaderos_api.repository.TipoParqueaderoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-parqueadero")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TipoParqueaderoController {

    private final TipoParqueaderoRepository tipoParqueaderoRepository;

    @GetMapping
    public ResponseEntity<List<TipoParqueadero>> getAll() {
        return ResponseEntity.ok(tipoParqueaderoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoParqueadero> getById(@PathVariable Long id) {
        return tipoParqueaderoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TipoParqueadero> create(@RequestBody TipoParqueadero tipoParqueadero) {
        return ResponseEntity.ok(tipoParqueaderoRepository.save(tipoParqueadero));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoParqueadero> update(@PathVariable Long id, @RequestBody TipoParqueadero tipoParqueadero) {
        if (!tipoParqueaderoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoParqueadero.setId(id);
        return ResponseEntity.ok(tipoParqueaderoRepository.save(tipoParqueadero));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!tipoParqueaderoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoParqueaderoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
