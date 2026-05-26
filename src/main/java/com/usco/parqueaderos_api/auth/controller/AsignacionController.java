package com.usco.parqueaderos_api.auth.controller;

import com.usco.parqueaderos_api.auth.entity.UsuarioParqueadero;
import com.usco.parqueaderos_api.auth.service.UsuarioParqueaderoService;
import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/asignaciones")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','ADMIN_PARQUEADERO')")
public class AsignacionController {

    private final UsuarioParqueaderoService service;

    /** Asigna un rol (ADMIN_PARQUEADERO u OPERARIO_CAJA) a un usuario en un parqueadero. */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> asignar(@RequestBody AsignarRequest req) {
        UsuarioParqueadero up = service.asignar(req.getUsuarioId(), req.getParqueaderoId(), req.getRolId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "usuarioId", up.getUsuario().getId(),
                "parqueaderoId", up.getParqueadero().getId(),
                "rolId", up.getRol().getId(),
                "rolNombre", up.getRol().getNombre(),
                "activo", up.getActivo(),
                "asignadoEn", up.getAsignadoEn().toString()
        )));
    }

    /** Desasigna (soft, con motivo) a un usuario de un parqueadero/rol. */
    @DeleteMapping("/usuario/{usuarioId}/parqueadero/{parqueaderoId}/rol/{rolId}")
    public ResponseEntity<ApiResponse<Void>> desasignar(
            @PathVariable Long usuarioId,
            @PathVariable Long parqueaderoId,
            @PathVariable Long rolId,
            @RequestParam String motivo) {
        service.desasignar(usuarioId, parqueaderoId, rolId, motivo);
        return ResponseEntity.ok(ApiResponse.ok(null, "Asignacion desactivada"));
    }

    /** Lista todas las asignaciones de un usuario (activas e historicas). */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> deUsuario(@PathVariable Long usuarioId) {
        List<Map<String, Object>> out = service.asignacionesDeUsuario(usuarioId).stream()
                .map(up -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("parqueaderoId", up.getParqueadero() != null ? up.getParqueadero().getId() : null);
                    m.put("parqueaderoNombre", up.getParqueadero() != null ? up.getParqueadero().getNombre() : null);
                    m.put("rolId", up.getRol() != null ? up.getRol().getId() : null);
                    m.put("rolNombre", up.getRol() != null ? up.getRol().getNombre() : null);
                    m.put("activo", up.getActivo());
                    m.put("asignadoEn", up.getAsignadoEn() != null ? up.getAsignadoEn().toString() : null);
                    m.put("desasignadoEn", up.getDesasignadoEn() != null ? up.getDesasignadoEn().toString() : null);
                    m.put("motivoDesasignacion", up.getMotivoDesasignacion());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    @Data
    public static class AsignarRequest {
        private Long usuarioId;
        private Long parqueaderoId;
        private Long rolId;
    }
}
