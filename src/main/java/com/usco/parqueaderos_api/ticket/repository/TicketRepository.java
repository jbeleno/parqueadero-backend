package com.usco.parqueaderos_api.ticket.repository;

import com.usco.parqueaderos_api.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByParqueaderoId(Long parqueaderoId);
    List<Ticket> findByParqueaderoIdIn(List<Long> parqueaderoIds);
    List<Ticket> findByParqueaderoEmpresaId(Long empresaId);
    List<Ticket> findByVehiculoId(Long vehiculoId);
    List<Ticket> findByVehiculoPersonaId(Long personaId);
    List<Ticket> findByEstado(String estado);

    @Query("SELECT COUNT(t) FROM Ticket t " +
           "WHERE t.parqueadero.id = :parqueaderoId AND t.estado = 'EN_CURSO'")
    long countOcupadosEnParqueadero(@Param("parqueaderoId") Long parqueaderoId);

    boolean existsByPuntoParqueoIdAndEstado(Long puntoParqueoId, String estado);

    /** Existe un ticket del vehiculo en el parqueadero con el estado dado (EN_CURSO tipico). */
    boolean existsByVehiculoIdAndParqueaderoIdAndEstado(Long vehiculoId, Long parqueaderoId, String estado);

    /** El ticket EN_CURSO mas reciente del vehiculo en el parqueadero. */
    java.util.Optional<Ticket> findFirstByVehiculoIdAndParqueaderoIdAndEstadoOrderByFechaHoraEntradaDesc(
            Long vehiculoId, Long parqueaderoId, String estado);

    /**
     * Ultimo ticket (de cualquier estado) del vehiculo en el parqueadero,
     * ordenado por fechaHoraEntrada desc. Sirve para diferenciar entre:
     * - salida fisica esperada (ticket recien cerrado),
     * - alerta de salida fantasma (sin ticket reciente).
     */
    java.util.Optional<Ticket> findFirstByVehiculoIdAndParqueaderoIdOrderByFechaHoraEntradaDesc(
            Long vehiculoId, Long parqueaderoId);

    /** Tickets EN_CURSO con entrada anterior a la fecha dada (candidatos a abandono). */
    @Query("SELECT t FROM Ticket t WHERE t.estado = 'EN_CURSO' " +
           "AND t.fechaHoraEntrada < :fechaCorte")
    List<Ticket> findEnCursoEntradaAntesDe(@Param("fechaCorte") java.time.LocalDateTime fechaCorte);

    long countByVehiculoId(Long vehiculoId);

    /**
     * Tickets CERRADOS con monto > 0, sin suscripcion, cuyo fecha_hora_salida
     * esta dentro del rango. Para backfill de facturas faltantes.
     * Si parqueaderoId es null, busca en todos.
     */
    @Query("SELECT t FROM Ticket t WHERE t.estado = 'CERRADO' " +
           "AND t.montoCalculado > 0 " +
           "AND t.suscripcionId IS NULL " +
           "AND (:parqueaderoId IS NULL OR t.parqueadero.id = :parqueaderoId) " +
           "AND t.fechaHoraSalida BETWEEN :desde AND :hasta " +
           "ORDER BY t.fechaHoraSalida ASC")
    List<Ticket> findCerradosFacturables(
            @Param("parqueaderoId") Long parqueaderoId,
            @Param("desde") java.time.LocalDateTime desde,
            @Param("hasta") java.time.LocalDateTime hasta);
}
