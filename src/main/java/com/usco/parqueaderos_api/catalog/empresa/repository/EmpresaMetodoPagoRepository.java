package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.EmpresaMetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaMetodoPagoRepository extends JpaRepository<EmpresaMetodoPago, Long> {
    Optional<EmpresaMetodoPago> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<EmpresaMetodoPago> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<EmpresaMetodoPago> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
