package com.usco.parqueaderos_api.location.repository;

import com.usco.parqueaderos_api.location.entity.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CiudadRepository extends JpaRepository<Ciudad, Long> {
    List<Ciudad> findByDepartamentoId(Long departamentoId);
}
