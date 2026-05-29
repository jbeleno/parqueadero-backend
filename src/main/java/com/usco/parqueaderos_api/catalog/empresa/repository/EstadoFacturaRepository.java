package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.EstadoFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoFacturaRepository extends JpaRepository<EstadoFactura, Long> {
    Optional<EstadoFactura> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<EstadoFactura> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<EstadoFactura> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
