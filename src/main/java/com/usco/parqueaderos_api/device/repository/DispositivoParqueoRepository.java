package com.usco.parqueaderos_api.device.repository;

import com.usco.parqueaderos_api.device.entity.DispositivoParqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispositivoParqueoRepository extends JpaRepository<DispositivoParqueo, Long> {
    List<DispositivoParqueo> findByDispositivoId(Long dispositivoId);
    List<DispositivoParqueo> findByPuntoParqueoId(Long puntoParqueoId);
}
