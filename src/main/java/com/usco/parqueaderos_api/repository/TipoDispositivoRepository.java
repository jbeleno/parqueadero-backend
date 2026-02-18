package com.usco.parqueaderos_api.repository;

import com.usco.parqueaderos_api.entity.TipoDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoDispositivoRepository extends JpaRepository<TipoDispositivo, Long> {
}
