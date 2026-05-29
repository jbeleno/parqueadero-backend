package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.TipoDescuentoConvenio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoDescuentoConvenioRepository extends JpaRepository<TipoDescuentoConvenio, Long> {
    Optional<TipoDescuentoConvenio> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<TipoDescuentoConvenio> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<TipoDescuentoConvenio> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
