package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.UnidadTarifa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnidadTarifaRepository extends JpaRepository<UnidadTarifa, Long> {
    Optional<UnidadTarifa> findByCodigo(String codigo);
    List<UnidadTarifa> findByActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items globales canonicos (empresa_id IS NULL). */
    java.util.List<UnidadTarifa> findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items custom de una empresa. */
    java.util.List<UnidadTarifa> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);

    /** v50: TODO (admin): globales + custom de la empresa, incluyendo inactivos. */
    java.util.List<UnidadTarifa> findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
