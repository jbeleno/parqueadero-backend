package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.CanalOrigenReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CanalOrigenReservaRepository extends JpaRepository<CanalOrigenReserva, Long> {
    Optional<CanalOrigenReserva> findByCodigo(String codigo);
    List<CanalOrigenReserva> findByActivoTrueOrderByOrdenDisplayAsc();
}
