package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.RegimenTributario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegimenTributarioRepository extends JpaRepository<RegimenTributario, Long> {
    Optional<RegimenTributario> findByCodigo(String codigo);
    List<RegimenTributario> findByActivoTrueOrderByOrdenDisplayAsc();
}
