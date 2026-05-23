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
    List<Ticket> findByParqueaderoEmpresaId(Long empresaId);
    List<Ticket> findByVehiculoId(Long vehiculoId);
    List<Ticket> findByVehiculoPersonaId(Long personaId);
    List<Ticket> findByEstado(String estado);

    @Query("SELECT COUNT(t) FROM Ticket t " +
           "WHERE t.parqueadero.id = :parqueaderoId AND t.estado = 'EN_CURSO'")
    long countOcupadosEnParqueadero(@Param("parqueaderoId") Long parqueaderoId);

    boolean existsByPuntoParqueoIdAndEstado(Long puntoParqueoId, String estado);

    /**
     * Ultimo ticket (de cualquier estado) del vehiculo en el parqueadero,
     * ordenado por fechaHoraEntrada desc. Sirve para diferenciar entre:
     * - salida fisica esperada (ticket recien cerrado),
     * - alerta de salida fantasma (sin ticket reciente).
     */
    java.util.Optional<Ticket> findFirstByVehiculoIdAndParqueaderoIdOrderByFechaHoraEntradaDesc(
            Long vehiculoId, Long parqueaderoId);
}
