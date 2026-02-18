package com.usco.parqueaderos_api.ticket.repository;

import com.usco.parqueaderos_api.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByParqueaderoId(Long parqueaderoId);
    List<Ticket> findByVehiculoId(Long vehiculoId);
    List<Ticket> findByEstado(String estado);
}
