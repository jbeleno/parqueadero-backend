package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.Camino;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaminoRepository extends JpaRepository<Camino, Long> {
    List<Camino> findByNivelId(Long nivelId);
    List<Camino> findByNivelIdAndEstadoNombreNot(Long nivelId, String estadoNombre);
}
