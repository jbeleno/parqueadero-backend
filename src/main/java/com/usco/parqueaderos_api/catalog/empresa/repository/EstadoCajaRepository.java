package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoCajaRepository extends JpaRepository<EstadoCaja, Long> {
    Optional<EstadoCaja> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<EstadoCaja> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<EstadoCaja> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
