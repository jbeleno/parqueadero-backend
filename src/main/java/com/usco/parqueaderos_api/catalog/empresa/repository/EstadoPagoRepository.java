package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoPagoRepository extends JpaRepository<EstadoPago, Long> {
    Optional<EstadoPago> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<EstadoPago> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<EstadoPago> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
