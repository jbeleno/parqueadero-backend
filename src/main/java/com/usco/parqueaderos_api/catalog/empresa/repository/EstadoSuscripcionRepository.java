package com.usco.parqueaderos_api.catalog.empresa.repository;

import com.usco.parqueaderos_api.catalog.empresa.entity.EstadoSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoSuscripcionRepository extends JpaRepository<EstadoSuscripcion, Long> {
    Optional<EstadoSuscripcion> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
    List<EstadoSuscripcion> findByEmpresaIdAndActivoTrueOrderByOrdenDisplayAsc(Long empresaId);
    List<EstadoSuscripcion> findByEmpresaIdOrderByOrdenDisplayAsc(Long empresaId);
}
