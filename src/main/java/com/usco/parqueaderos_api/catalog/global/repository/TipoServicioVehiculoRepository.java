package com.usco.parqueaderos_api.catalog.global.repository;

import com.usco.parqueaderos_api.catalog.global.entity.TipoServicioVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoServicioVehiculoRepository extends JpaRepository<TipoServicioVehiculo, Long> {
    Optional<TipoServicioVehiculo> findByCodigo(String codigo);
    List<TipoServicioVehiculo> findByActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items globales canonicos (empresa_id IS NULL). */
    java.util.List<TipoServicioVehiculo> findByEmpresaIdIsNullAndActivoTrueOrderByOrdenDisplayAsc();

    /** v50: items custom de una empresa. */
    java.util.List<TipoServicioVehiculo> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);

    /** v50: TODO (admin): globales + custom de la empresa, incluyendo inactivos. */
    java.util.List<TipoServicioVehiculo> findByEmpresaIdIsNullOrEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
