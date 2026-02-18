package com.usco.parqueaderos_api.repository;

import com.usco.parqueaderos_api.entity.PuntoParqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PuntoParqueoRepository extends JpaRepository<PuntoParqueo, Long> {
}
