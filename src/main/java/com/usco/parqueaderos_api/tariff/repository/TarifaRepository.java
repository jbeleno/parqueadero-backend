package com.usco.parqueaderos_api.tariff.repository;

import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {
    List<Tarifa> findByParqueaderoId(Long parqueaderoId);
}
