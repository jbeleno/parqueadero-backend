package com.usco.parqueaderos_api.config.repository;

import com.usco.parqueaderos_api.config.entity.EmpresaValidacionCampo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaValidacionCampoRepository extends JpaRepository<EmpresaValidacionCampo, Long> {
    Optional<EmpresaValidacionCampo> findByEmpresaIdAndEntidadAndCampo(Long empresaId, String entidad, String campo);
    List<EmpresaValidacionCampo> findByEmpresaIdAndActivaTrueOrderByEntidadAscCampoAsc(Long empresaId);
    List<EmpresaValidacionCampo> findByEmpresaIdAndEntidadAndActivaTrueOrderByCampoAsc(Long empresaId, String entidad);
}
