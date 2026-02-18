package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    List<Seccion> findByParqueaderoId(Long parqueaderoId);
    List<Seccion> findByNivelId(Long nivelId);
    List<Seccion> findByNivelIdAndEstadoNombreNot(Long nivelId, String estadoNombre);
}
