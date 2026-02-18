package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.TipoPuntoParqueo;
import com.usco.parqueaderos_api.repository.TipoPuntoParqueoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-punto-parqueo")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TipoPuntoParqueoController {

    private final TipoPuntoParqueoRepository tipoPuntoParqueoRepository;

    @GetMapping
    public ResponseEntity<List<TipoPuntoParqueo>> getAll() {
        return ResponseEntity.ok(tipoPuntoParqueoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoPuntoParqueo> getById(@PathVariable Long id) {
        return tipoPuntoParqueoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TipoPuntoParqueo> create(@RequestBody TipoPuntoParqueo tipoPuntoParqueo) {
        return ResponseEntity.ok(tipoPuntoParqueoRepository.save(tipoPuntoParqueo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoPuntoParqueo> update(@PathVariable Long id, @RequestBody TipoPuntoParqueo tipoPuntoParqueo) {
        if (!tipoPuntoParqueoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoPuntoParqueo.setId(id);
        return ResponseEntity.ok(tipoPuntoParqueoRepository.save(tipoPuntoParqueo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!tipoPuntoParqueoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoPuntoParqueoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
