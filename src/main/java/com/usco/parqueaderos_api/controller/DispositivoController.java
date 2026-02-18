package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Dispositivo;
import com.usco.parqueaderos_api.repository.DispositivoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dispositivos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DispositivoController {

    private final DispositivoRepository dispositivoRepository;

    @GetMapping
    public ResponseEntity<List<Dispositivo>> getAll() {
        return ResponseEntity.ok(dispositivoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dispositivo> getById(@PathVariable Long id) {
        return dispositivoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Dispositivo> create(@RequestBody Dispositivo dispositivo) {
        return ResponseEntity.ok(dispositivoRepository.save(dispositivo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dispositivo> update(@PathVariable Long id, @RequestBody Dispositivo dispositivo) {
        if (!dispositivoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dispositivo.setId(id);
        return ResponseEntity.ok(dispositivoRepository.save(dispositivo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!dispositivoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dispositivoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
