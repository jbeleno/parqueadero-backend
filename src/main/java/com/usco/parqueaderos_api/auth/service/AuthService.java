package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.auth.dto.AuthRequest;
import com.usco.parqueaderos_api.auth.dto.AuthResponse;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));

        UserDetails userDetails = User.builder()
                .username(usuario.getCorreo())
                .password(usuario.getPassword())
                .roles("USER")
                .build();

        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .correo(usuario.getCorreo())
                .tipo("Bearer")
                .build();
    }
}
