package com.usco.parqueaderos_api.subscription.repository;

import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.Suscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    List<Suscripcion> findByVehiculoId(Long vehiculoId);

    List<Suscripcion> findByParqueaderoIdAndEstado(Long parqueaderoId, EstadoSuscripcion estado);

    List<Suscripcion> findByParqueaderoEmpresaId(Long empresaId);

    /** Suscripcion ACTIVA de un tipo especifico para (vehiculo, parqueadero). */
    Optional<Suscripcion> findFirstByVehiculoIdAndParqueaderoIdAndTipoAndEstado(
            Long vehiculoId, Long parqueaderoId, TipoSuscripcion tipo, EstadoSuscripcion estado);

    /** Todas las suscripciones ACTIVAS del vehiculo en el parqueadero, ordenadas por tipo. */
    List<Suscripcion> findByVehiculoIdAndParqueaderoIdAndEstadoOrderByTipoAsc(
            Long vehiculoId, Long parqueaderoId, EstadoSuscripcion estado);

    /** Lock pesimista para descuento concurrente de saldo en ABONO_PREPAGO. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Suscripcion s WHERE s.id = :id")
    Optional<Suscripcion> findByIdForUpdate(@Param("id") Long id);

    /** Suscripciones ACTIVAS cuyo fecha_fin ya paso. Job @Scheduled las marca VENCIDA. */
    @Query("SELECT s FROM Suscripcion s WHERE s.estado = 'ACTIVA' AND s.fechaFin < :ahora")
    List<Suscripcion> findActivasVencidas(@Param("ahora") LocalDateTime ahora);

    /** Suscripciones ACTIVAS que vencen entre ahora y ahora+N dias. Para alertas. */
    @Query("SELECT s FROM Suscripcion s WHERE s.estado = 'ACTIVA' " +
           "AND s.fechaFin BETWEEN :ahora AND :limite " +
           "AND (:parqueaderoId IS NULL OR s.parqueadero.id = :parqueaderoId)")
    List<Suscripcion> findProximasAVencer(
            @Param("ahora") LocalDateTime ahora,
            @Param("limite") LocalDateTime limite,
            @Param("parqueaderoId") Long parqueaderoId);

    long countByParqueaderoIdAndEstado(Long parqueaderoId, EstadoSuscripcion estado);
}
