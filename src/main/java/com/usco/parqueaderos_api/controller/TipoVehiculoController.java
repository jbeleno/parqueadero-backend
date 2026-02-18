package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.TipoVehiculo;
import com.usco.parqueaderos_api.repository.TipoVehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-vehiculo")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TipoVehiculoController {

    private final TipoVehiculoRepository tipoVehiculoRepository;

    @GetMapping
    public ResponseEntity<List<TipoVehiculo>> getAll() {
        return ResponseEntity.ok(tipoVehiculoRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoVehiculo> getById(@PathVariable Long id) {
        return tipoVehiculoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TipoVehiculo> create(@RequestBody TipoVehiculo tipoVehiculo) {
        return ResponseEntity.ok(tipoVehiculoRepository.save(tipoVehiculo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoVehiculo> update(@PathVariable Long id, @RequestBody TipoVehiculo tipoVehiculo) {
        if (!tipoVehiculoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoVehiculo.setId(id);
        return ResponseEntity.ok(tipoVehiculoRepository.save(tipoVehiculo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!tipoVehiculoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tipoVehiculoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
