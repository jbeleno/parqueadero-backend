package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PinService {

    private final UsuarioRepository usuarioRepository;

    @Value("${app.pin.expiration-minutes:15}")
    private int expirationMinutes;

    private static final SecureRandom random = new SecureRandom();

    public String generarPin() {
        int number = random.nextInt(900000) + 100000; // 100000 - 999999
        return String.valueOf(number);
    }

    @Transactional
    public String guardarPin(Usuario usuario) {
        String pin = generarPin();
        usuario.setPinCodigo(pin);
        usuario.setPinExpiracion(LocalDateTime.now().plusMinutes(expirationMinutes));
        usuarioRepository.save(usuario);
        return pin;
    }

    public boolean verificarPin(Usuario usuario, String pin) {
        if (usuario.getPinCodigo() == null || usuario.getPinExpiracion() == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(usuario.getPinExpiracion())) {
            return false; // Expirado
        }
        return usuario.getPinCodigo().equals(pin);
    }

    @Transactional
    public void limpiarPin(Usuario usuario) {
        usuario.setPinCodigo(null);
        usuario.setPinExpiracion(null);
        usuarioRepository.save(usuario);
    }
}
