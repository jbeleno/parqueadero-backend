package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.UnidadTarifa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnidadTarifaRepository extends JpaRepository<UnidadTarifa, Long> {
    Optional<UnidadTarifa> findByCodigo(String codigo);
    List<UnidadTarifa> findByActivoTrueOrderByOrdenDisplayAsc();
}
