package com.usco.parqueaderos_api.reservation.repository;

import com.usco.parqueaderos_api.reservation.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByUsuarioId(Long usuarioId);
    List<Reserva> findByParqueaderoId(Long parqueaderoId);
    List<Reserva> findByParqueaderoEmpresaId(Long empresaId);
    List<Reserva> findByEstado(String estado);

    @Query("SELECT COUNT(r) FROM Reserva r " +
           "WHERE r.parqueadero.id = :parqueaderoId " +
           "AND r.estado IN ('PENDIENTE', 'CONFIRMADA') " +
           "AND r.puntoParqueo IS NOT NULL " +
           "AND :ahora BETWEEN r.fechaHoraInicio AND r.fechaHoraFin")
    long countReservadosEnParqueadero(@Param("parqueaderoId") Long parqueaderoId,
                                      @Param("ahora") LocalDateTime ahora);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Reserva r " +
           "WHERE r.puntoParqueo.id = :puntoId " +
           "AND r.estado IN ('PENDIENTE', 'CONFIRMADA') " +
           "AND :ahora BETWEEN r.fechaHoraInicio AND r.fechaHoraFin")
    boolean existsReservaActivaParaPunto(@Param("puntoId") Long puntoId,
                                          @Param("ahora") LocalDateTime ahora);

    long countByVehiculoId(Long vehiculoId);
}
