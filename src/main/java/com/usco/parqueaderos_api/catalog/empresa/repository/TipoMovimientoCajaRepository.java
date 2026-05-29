package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.TipoMovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoMovimientoCajaRepository extends JpaRepository<TipoMovimientoCaja, Long> {
    Optional<TipoMovimientoCaja> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<TipoMovimientoCaja> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<TipoMovimientoCaja> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
