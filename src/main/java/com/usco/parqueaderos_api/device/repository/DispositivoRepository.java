package com.usco.parqueaderos_api.device.repository;

import com.usco.parqueaderos_api.device.entity.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {
    List<Dispositivo> findByParqueaderoId(Long parqueaderoId);
}
