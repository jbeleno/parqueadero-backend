package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {
    Optional<TipoDocumento> findByCodigo(String codigo);
    List<TipoDocumento> findByActivoTrueOrderByOrdenDisplayAsc();
}
