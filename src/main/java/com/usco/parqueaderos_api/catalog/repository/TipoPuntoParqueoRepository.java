package com.usco.parqueaderos_api.catalog.repository;

import com.usco.parqueaderos_api.catalog.entity.TipoPuntoParqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoPuntoParqueoRepository extends JpaRepository<TipoPuntoParqueo, Long> {
    Optional<TipoPuntoParqueo> findByNombreIgnoreCase(String nombre);
}
