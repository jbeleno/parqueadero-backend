package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParqueaderoRepository extends JpaRepository<Parqueadero, Long> {
    List<Parqueadero> findByEmpresaId(Long empresaId);
    List<Parqueadero> findByCiudadId(Long ciudadId);
}
