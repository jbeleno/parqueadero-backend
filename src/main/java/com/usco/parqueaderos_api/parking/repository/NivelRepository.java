package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.Nivel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NivelRepository extends JpaRepository<Nivel, Long> {
    List<Nivel> findByParqueaderoId(Long parqueaderoId);
    List<Nivel> findByParqueaderoIdAndEstadoNombreNot(Long parqueaderoId, String estadoNombre);
}
