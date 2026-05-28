package com.usco.parqueaderos_api.billing.repository;

import com.usco.parqueaderos_api.billing.entity.ResolucionDian;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResolucionDianRepository extends JpaRepository<ResolucionDian, Long> {

    /** Todas las NO archivadas de un parqueadero, ordenadas por vigencia desc (la mas nueva primero). */
    @Query("SELECT r FROM ResolucionDian r " +
           "WHERE r.parqueadero.id = :parqueaderoId AND r.archivadaEn IS NULL " +
           "ORDER BY r.vigenteDesde DESC")
    List<ResolucionDian> findNoArchivadasPorParqueadero(@Param("parqueaderoId") Long parqueaderoId);

    /** La marcada como principal del parqueadero (la que usa AutoFacturaListener). */
    @Query("SELECT r FROM ResolucionDian r " +
           "WHERE r.parqueadero.id = :parqueaderoId " +
           "  AND r.principal = TRUE AND r.archivadaEn IS NULL")
    Optional<ResolucionDian> findPrincipalDeParqueadero(@Param("parqueaderoId") Long parqueaderoId);

    /** Bulk desactivar principal de un parqueadero (al marcar otra como principal). */
    @Modifying
    @Query("UPDATE ResolucionDian r SET r.principal = FALSE " +
           "WHERE r.parqueadero.id = :parqueaderoId AND r.principal = TRUE")
    int desactivarPrincipalDeParqueadero(@Param("parqueaderoId") Long parqueaderoId);

    /** Lock pesimista para auto-incrementar consecutivo de forma serial. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ResolucionDian r WHERE r.id = :id")
    Optional<ResolucionDian> findByIdForUpdate(@Param("id") Long id);

    /** Saber si un parqueadero ya tiene alguna resolucion (para alertar primera vez). */
    boolean existsByParqueaderoIdAndArchivadaEnIsNull(Long parqueaderoId);
}
