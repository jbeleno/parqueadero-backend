package com.usco.parqueaderos_api.reservation.repository;

import com.usco.parqueaderos_api.reservation.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByUsuarioId(Long usuarioId);
    List<Reserva> findByParqueaderoId(Long parqueaderoId);
    List<Reserva> findByEstado(String estado);
}
