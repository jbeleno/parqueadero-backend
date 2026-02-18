package com.usco.parqueaderos_api.repository;

import com.usco.parqueaderos_api.entity.SubSeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubSeccionRepository extends JpaRepository<SubSeccion, Long> {
}
