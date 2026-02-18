package com.usco.parqueaderos_api.repository;

import com.usco.parqueaderos_api.entity.TipoPuntoParqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoPuntoParqueoRepository extends JpaRepository<TipoPuntoParqueo, Long> {
}
