package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.auth.entity.RefreshToken;
import com.usco.parqueaderos_api.auth.repository.RefreshTokenRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.user.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken crearRefreshToken(Usuario usuario) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .usuario(usuario)
                .expiracion(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .revocado(false)
                .creadoEn(LocalDateTime.now())
                .build();
        return refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken verificarRefreshToken(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Refresh token no encontrado o inválido"));
        if (rt.getRevocado()) {
            throw new BusinessException("Refresh token revocado");
        }
        if (LocalDateTime.now().isAfter(rt.getExpiracion())) {
            throw new BusinessException("Refresh token expirado, por favor inicia sesión nuevamente");
        }
        return rt;
    }

    @Transactional
    public void revocarTokensDeUsuario(Long usuarioId) {
        refreshTokenRepository.revocarTokensPorUsuario(usuarioId);
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?") // Cada día a las 3am
    public void limpiarTokensExpirados() {
        refreshTokenRepository.deleteByExpiracionBefore(LocalDateTime.now());
        log.info("Refresh tokens expirados eliminados");
    }
}
