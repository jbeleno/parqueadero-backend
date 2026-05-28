package com.usco.parqueaderos_api.user.service;

import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el nombre legible de un usuario para mostrar en DTOs sin
 * forzar al frontend a hacer un GET /api/usuarios extra.
 *
 * Cache: 'usuarioNombre' configurado en CacheConfig. TTL razonable
 * (no se invalida con cambios de nombre/correo, asumimos que son raros).
 */
@Service
@RequiredArgsConstructor
public class UsuarioNombreResolver {

    private final UsuarioRepository usuarioRepository;

    /**
     * Devuelve "Nombre Apellido" si hay persona asociada, sino el correo,
     * sino null. Cachea para no repetir queries en listados.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "usuarioNombre", unless = "#result == null")
    public String nombreOf(Long usuarioId) {
        if (usuarioId == null) return null;
        return usuarioRepository.findById(usuarioId)
                .map(this::format)
                .orElse(null);
    }

    private String format(Usuario u) {
        Persona p = u.getPersona();
        if (p != null) {
            String n = p.getNombre() == null ? "" : p.getNombre().trim();
            String a = p.getApellido() == null ? "" : p.getApellido().trim();
            String full = (n + " " + a).trim();
            if (!full.isEmpty()) return full;
        }
        return u.getCorreo();
    }
}
