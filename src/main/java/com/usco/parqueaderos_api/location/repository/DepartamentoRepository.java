package com.usco.parqueaderos_api.location.repository;

import com.usco.parqueaderos_api.location.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Long> {
    List<Departamento> findByPaisId(Long paisId);
}
