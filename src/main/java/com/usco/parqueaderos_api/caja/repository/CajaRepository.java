package com.usco.parqueaderos_api.caja.repository;

import com.usco.parqueaderos_api.caja.entity.Caja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {
    Optional<Caja> findFirstByUsuarioIdAndEstado(Long usuarioId, String estado);
    List<Caja> findByUsuarioIdOrderByAbiertaEnDesc(Long usuarioId);
    List<Caja> findByParqueaderoIdAndEstado(Long parqueaderoId, String estado);
    List<Caja> findByParqueaderoIdOrderByAbiertaEnDesc(Long parqueaderoId);
}
