package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.EstadoCivil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoCivilRepository extends JpaRepository<EstadoCivil, Long> {
    Optional<EstadoCivil> findByCodigo(String codigo);
    List<EstadoCivil> findByActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items globales canonicos (empresa_id IS NULL). */
    java.util.List<EstadoCivil> findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items custom de una empresa. */
    java.util.List<EstadoCivil> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);

    /** v50: TODO (admin): globales + custom de la empresa, incluyendo inactivos. */
    java.util.List<EstadoCivil> findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
