package com.usco.parqueaderos_api.repository;

import com.usco.parqueaderos_api.entity.TipoParqueadero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoParqueaderoRepository extends JpaRepository<TipoParqueadero, Long> {
}
