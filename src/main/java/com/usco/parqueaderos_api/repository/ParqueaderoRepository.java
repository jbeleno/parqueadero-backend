package com.usco.parqueaderos_api.repository;

import com.usco.parqueaderos_api.entity.Parqueadero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParqueaderoRepository extends JpaRepository<Parqueadero, Long> {
}
