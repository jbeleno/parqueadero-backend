package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.TipoAccesoDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoAccesoDispositivoRepository extends JpaRepository<TipoAccesoDispositivo, Long> {
    Optional<TipoAccesoDispositivo> findByCodigo(String codigo);
    List<TipoAccesoDispositivo> findByActivoTrueOrderByOrdenDisplayAsc();
}
