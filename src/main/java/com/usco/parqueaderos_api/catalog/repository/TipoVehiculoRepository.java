package com.usco.parqueaderos_api.catalog.repository;

import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoVehiculoRepository extends JpaRepository<TipoVehiculo, Long> {

    /** Default para vehiculos invitados creados desde OCR. */
    java.util.Optional<TipoVehiculo> findFirstByOrderByIdAsc();
}
