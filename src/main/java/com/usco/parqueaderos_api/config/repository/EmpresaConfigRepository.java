package com.usco.parqueaderos_api.config.repository;

import com.usco.parqueaderos_api.config.entity.EmpresaConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmpresaConfigRepository extends JpaRepository<EmpresaConfig, Long> {
    Optional<EmpresaConfig> findByEmpresaIdAndClave(Long empresaId, String clave);
    List<EmpresaConfig> findByEmpresaIdOrderByCategoriaAscClaveAsc(Long empresaId);
    List<EmpresaConfig> findByEmpresaIdAndCategoriaOrderByClaveAsc(Long empresaId, String categoria);
}
