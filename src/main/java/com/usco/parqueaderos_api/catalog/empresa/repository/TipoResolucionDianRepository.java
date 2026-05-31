package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.TipoResolucionDian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoResolucionDianRepository extends JpaRepository<TipoResolucionDian, Long> {
    Optional<TipoResolucionDian> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<TipoResolucionDian> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<TipoResolucionDian> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
