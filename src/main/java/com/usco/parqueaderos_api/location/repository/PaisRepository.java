package com.usco.parqueaderos_api.location.repository;

import com.usco.parqueaderos_api.location.entity.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaisRepository extends JpaRepository<Pais, Long> {
}
