package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.dto.ParqueaderoDTO;
import com.usco.parqueaderos_api.service.ParqueaderoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parqueaderos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ParqueaderoController {
    
    private final ParqueaderoService parqueaderoService;
    
    @GetMapping
    public ResponseEntity<List<ParqueaderoDTO>> getAll() {
        return ResponseEntity.ok(parqueaderoService.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ParqueaderoDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(parqueaderoService.findById(id));
    }
    
    @PostMapping
    public ResponseEntity<ParqueaderoDTO> create(@RequestBody ParqueaderoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parqueaderoService.save(dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ParqueaderoDTO> update(@PathVariable Long id, @RequestBody ParqueaderoDTO dto) {
        return ResponseEntity.ok(parqueaderoService.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parqueaderoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
