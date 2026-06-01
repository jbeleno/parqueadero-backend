package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.RegimenTributario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegimenTributarioRepository extends JpaRepository<RegimenTributario, Long> {
    Optional<RegimenTributario> findByCodigo(String codigo);
    List<RegimenTributario> findByActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items globales canonicos (empresa_id IS NULL). */
    java.util.List<RegimenTributario> findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items custom de una empresa. */
    java.util.List<RegimenTributario> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);

    /** v50: TODO (admin): globales + custom de la empresa, incluyendo inactivos. */
    java.util.List<RegimenTributario> findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
