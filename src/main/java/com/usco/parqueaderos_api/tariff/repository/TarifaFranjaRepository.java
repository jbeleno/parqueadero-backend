package com.usco.parqueaderos_api.tariff.repository;

import com.usco.parqueaderos_api.tariff.entity.TarifaFranja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarifaFranjaRepository extends JpaRepository<TarifaFranja, Long> {

    List<TarifaFranja> findByTarifaIdAndActivaTrue(Long tarifaId);

    List<TarifaFranja> findByTarifaId(Long tarifaId);
}
