package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.TipoMovimientoSaldo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoMovimientoSaldoRepository extends JpaRepository<TipoMovimientoSaldo, Long> {
    Optional<TipoMovimientoSaldo> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<TipoMovimientoSaldo> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<TipoMovimientoSaldo> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
