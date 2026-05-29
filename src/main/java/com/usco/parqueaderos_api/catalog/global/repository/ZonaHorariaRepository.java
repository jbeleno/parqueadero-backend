package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.ZonaHoraria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ZonaHorariaRepository extends JpaRepository<ZonaHoraria, Long> {
    Optional<ZonaHoraria> findByCodigo(String codigo);
    List<ZonaHoraria> findByActivoTrueOrderByOrdenDisplayAsc();
}
