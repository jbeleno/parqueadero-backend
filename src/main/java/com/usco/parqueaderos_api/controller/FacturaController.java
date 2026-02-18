package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Factura;
import com.usco.parqueaderos_api.repository.FacturaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaRepository facturaRepository;

    @GetMapping
    public ResponseEntity<List<Factura>> getAll() {
        return ResponseEntity.ok(facturaRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Factura> getById(@PathVariable Long id) {
        return facturaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Factura> create(@RequestBody Factura factura) {
        if (factura.getFechaHora() == null) {
            factura.setFechaHora(LocalDateTime.now());
        }
        return ResponseEntity.ok(facturaRepository.save(factura));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Factura> update(@PathVariable Long id, @RequestBody Factura factura) {
        if (!facturaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        factura.setId(id);
        return ResponseEntity.ok(facturaRepository.save(factura));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!facturaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        facturaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
