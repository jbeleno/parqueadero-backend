package com.usco.parqueaderos_api.convenio.repository;

import com.usco.parqueaderos_api.convenio.entity.Convenio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConvenioRepository extends JpaRepository<Convenio, Long> {
    List<Convenio> findByParqueaderoIdAndActivoTrue(Long parqueaderoId);
    List<Convenio> findByParqueaderoId(Long parqueaderoId);
    List<Convenio> findByParqueaderoEmpresaId(Long empresaId);
}
