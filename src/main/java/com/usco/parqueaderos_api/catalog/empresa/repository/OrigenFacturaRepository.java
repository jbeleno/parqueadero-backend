package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.OrigenFactura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrigenFacturaRepository extends JpaRepository<OrigenFactura, Long> {
    Optional<OrigenFactura> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<OrigenFactura> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<OrigenFactura> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
