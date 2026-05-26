package com.usco.parqueaderos_api.caja.repository;

import com.usco.parqueaderos_api.caja.entity.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    List<MovimientoCaja> findByCajaIdOrderByFechaHoraAsc(Long cajaId);
}
