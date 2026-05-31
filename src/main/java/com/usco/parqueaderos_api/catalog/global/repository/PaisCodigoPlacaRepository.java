package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.PaisCodigoPlaca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaisCodigoPlacaRepository extends JpaRepository<PaisCodigoPlaca, Long> {
    Optional<PaisCodigoPlaca> findByCodigo(String codigo);
    List<PaisCodigoPlaca> findByActivoTrueOrderByOrdenDisplayAsc();
}
