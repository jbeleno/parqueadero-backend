package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.user.entity.UsuarioRol;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import com.usco.parqueaderos_api.user.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var usuario = usuarioRepository.findByCorreo(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        List<UsuarioRol> usuarioRoles = usuarioRolRepository.findByUsuarioId(usuario.getId());

        List<SimpleGrantedAuthority> authorities;
        if (usuarioRoles.isEmpty()) {
            // Si no tiene roles asignados en DB, asignar USER por defecto
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            authorities = usuarioRoles.stream()
                    .map(ur -> new SimpleGrantedAuthority("ROLE_" + ur.getRol().getNombre().toUpperCase()))
                    .toList();
        }

        return User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}

