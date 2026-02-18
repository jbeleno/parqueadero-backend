package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Vehiculo;
import com.usco.parqueaderos_api.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;

    @GetMapping
    public ResponseEntity<List<Vehiculo>> getAll() {
        return ResponseEntity.ok(vehiculoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehiculo> getById(@PathVariable Long id) {
        return vehiculoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Vehiculo> create(@RequestBody Vehiculo vehiculo) {
        return ResponseEntity.ok(vehiculoRepository.save(vehiculo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> update(@PathVariable Long id, @RequestBody Vehiculo vehiculo) {
        if (!vehiculoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        vehiculo.setId(id);
        return ResponseEntity.ok(vehiculoRepository.save(vehiculo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!vehiculoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        vehiculoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
