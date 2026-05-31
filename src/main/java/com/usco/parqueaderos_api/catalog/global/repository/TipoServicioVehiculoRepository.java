package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.TipoServicioVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoServicioVehiculoRepository extends JpaRepository<TipoServicioVehiculo, Long> {
    Optional<TipoServicioVehiculo> findByCodigo(String codigo);
    List<TipoServicioVehiculo> findByActivoTrueOrderByOrdenDisplayAsc();
}
