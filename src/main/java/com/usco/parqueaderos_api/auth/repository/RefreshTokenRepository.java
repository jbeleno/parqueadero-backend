package com.usco.parqueaderos_api.auth.repository;

import com.usco.parqueaderos_api.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUsuarioIdAndRevocadoFalse(Long usuarioId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.usuario.id = :usuarioId AND r.revocado = false")
    void revocarTokensPorUsuario(Long usuarioId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiracion < :fecha")
    void deleteByExpiracionBefore(LocalDateTime fecha);
}
