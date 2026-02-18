package com.usco.parqueaderos_api.user.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.user.dto.PersonaDTO;
import com.usco.parqueaderos_api.user.dto.UsuarioDTO;
import com.usco.parqueaderos_api.user.service.PersonaService;
import com.usco.parqueaderos_api.user.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final PersonaService personaService;
    private final UsuarioService usuarioService;

    // ---- Personas ----
    @GetMapping("/api/personas")
    public ResponseEntity<ApiResponse<List<PersonaDTO>>> getAllPersonas() {
        return ResponseEntity.ok(ApiResponse.ok(personaService.findAll()));
    }

    @GetMapping("/api/personas/{id}")
    public ResponseEntity<ApiResponse<PersonaDTO>> getPersonaById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(personaService.findById(id)));
    }

    @PostMapping("/api/personas")
    public ResponseEntity<ApiResponse<PersonaDTO>> createPersona(@Valid @RequestBody PersonaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(personaService.save(dto)));
    }

    @PutMapping("/api/personas/{id}")
    public ResponseEntity<ApiResponse<PersonaDTO>> updatePersona(@PathVariable Long id, @Valid @RequestBody PersonaDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(personaService.update(id, dto)));
    }

    // ---- Usuarios ----
    @GetMapping("/api/usuarios")
    public ResponseEntity<ApiResponse<List<UsuarioDTO>>> getAllUsuarios() {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.findAll()));
    }

    @GetMapping("/api/usuarios/{id}")
    public ResponseEntity<ApiResponse<UsuarioDTO>> getUsuarioById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.findById(id)));
    }

    @PostMapping("/api/usuarios")
    public ResponseEntity<ApiResponse<UsuarioDTO>> createUsuario(@Valid @RequestBody UsuarioDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(usuarioService.save(dto)));
    }

    @PutMapping("/api/usuarios/{id}")
    public ResponseEntity<ApiResponse<UsuarioDTO>> updateUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.update(id, dto)));
    }
}
