package com.usco.parqueaderos_api.controller;

import com.usco.parqueaderos_api.entity.Persona;
import com.usco.parqueaderos_api.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaRepository personaRepository;

    @GetMapping
    public ResponseEntity<List<Persona>> getAll() {
        return ResponseEntity.ok(personaRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Persona> getById(@PathVariable Long id) {
        return personaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Persona> create(@RequestBody Persona persona) {
        return ResponseEntity.ok(personaRepository.save(persona));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Persona> update(@PathVariable Long id, @RequestBody Persona persona) {
        if (!personaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        persona.setId(id);
        return ResponseEntity.ok(personaRepository.save(persona));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!personaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        personaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
