package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.dto.TarifaDTO;
import com.usco.parqueaderos_api.service.TarifaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TarifaController {
    
    private final TarifaService tarifaService;
    
    @GetMapping
    public ResponseEntity<List<TarifaDTO>> getAll() {
        return ResponseEntity.ok(tarifaService.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TarifaDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tarifaService.findById(id));
    }
    
    @PostMapping
    public ResponseEntity<TarifaDTO> create(@RequestBody TarifaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tarifaService.save(dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TarifaDTO> update(@PathVariable Long id, @RequestBody TarifaDTO dto) {
        return ResponseEntity.ok(tarifaService.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tarifaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
