package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.TipoAccesoDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoAccesoDispositivoRepository extends JpaRepository<TipoAccesoDispositivo, Long> {
    Optional<TipoAccesoDispositivo> findByCodigo(String codigo);
    List<TipoAccesoDispositivo> findByActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items globales canonicos (empresa_id IS NULL). */
    java.util.List<TipoAccesoDispositivo> findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items custom de una empresa. */
    java.util.List<TipoAccesoDispositivo> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);

    /** v50: TODO (admin): globales + custom de la empresa, incluyendo inactivos. */
    java.util.List<TipoAccesoDispositivo> findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
