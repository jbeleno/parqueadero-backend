package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.Genero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeneroRepository extends JpaRepository<Genero, Long> {
    Optional<Genero> findByCodigo(String codigo);
    List<Genero> findByActivoTrueOrderByOrdenDisplayAsc();
}
