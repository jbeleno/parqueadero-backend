package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.TipoDispositivo;
import com.usco.parqueaderos_api.repository.TipoDispositivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-dispositivo")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TipoDispositivoController {

    private final TipoDispositivoRepository tipoDispositivoRepository;

    @GetMapping
    public ResponseEntity<List<TipoDispositivo>> getAll() {
        return ResponseEntity.ok(tipoDispositivoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoDispositivo> getById(@PathVariable Long id) {
        return tipoDispositivoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TipoDispositivo> create(@RequestBody TipoDispositivo tipoDispositivo) {
        return ResponseEntity.ok(tipoDispositivoRepository.save(tipoDispositivo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoDispositivo> update(@PathVariable Long id, @RequestBody TipoDispositivo tipoDispositivo) {
        if (!tipoDispositivoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoDispositivo.setId(id);
        return ResponseEntity.ok(tipoDispositivoRepository.save(tipoDispositivo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!tipoDispositivoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoDispositivoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
