package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.SubSeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubSeccionRepository extends JpaRepository<SubSeccion, Long> {
    List<SubSeccion> findBySeccionId(Long seccionId);
}
