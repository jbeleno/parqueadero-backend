package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PuntoParqueoRepository extends JpaRepository<PuntoParqueo, Long> {
    List<PuntoParqueo> findBySubSeccionId(Long subSeccionId);
}
