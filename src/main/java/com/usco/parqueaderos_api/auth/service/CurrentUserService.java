package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Helper para acceder al usuario autenticado y su contexto de filtrado multi-tenant.
 *
 * Reglas:
 * - SUPER_ADMIN  -> ve todo (sin filtro)
 * - ADMIN        -> filtra por su empresa (requiere empresaId definido)
 * - USER         -> filtra por su propio usuarioId / personaId
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Usuario getCurrent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException("Usuario no autenticado");
        }
        String correo = auth.getName();
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no existe en BD"));
    }

    @Transactional(readOnly = true)
    public Long getCurrentUserId() {
        return getCurrent().getId();
    }

    @Transactional(readOnly = true)
    public Optional<Long> getCurrentEmpresaId() {
        Usuario u = getCurrent();
        return Optional.ofNullable(u.getEmpresa()).map(e -> e.getId());
    }

    @Transactional(readOnly = true)
    public Long getCurrentPersonaId() {
        return getCurrent().getPersona().getId();
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String full = "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(full::equals);
    }

    public boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isUser() {
        return hasRole("USER");
    }
}
