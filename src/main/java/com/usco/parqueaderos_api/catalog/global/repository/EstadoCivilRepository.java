package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.EstadoCivil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoCivilRepository extends JpaRepository<EstadoCivil, Long> {
    Optional<EstadoCivil> findByCodigo(String codigo);
    List<EstadoCivil> findByActivoTrueOrderByOrdenDisplayAsc();
}
