package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.Camara;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CamaraRepository extends JpaRepository<Camara, Long> {
    List<Camara> findByNivelId(Long nivelId);
    List<Camara> findByNivelIdAndEstadoNombreNot(Long nivelId, String estadoNombre);
}
