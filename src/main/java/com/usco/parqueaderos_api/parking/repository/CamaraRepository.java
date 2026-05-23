package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.Camara;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CamaraRepository extends JpaRepository<Camara, Long> {
    List<Camara> findByNivelId(Long nivelId);
    List<Camara> findByNivelIdAndEstadoNombreNot(Long nivelId, String estadoNombre);

    @Modifying
    @Query("UPDATE Camara c SET c.estado.id = :archivadoId WHERE c.nivel.id = :nivelId")
    int archivarPorNivelId(@Param("nivelId") Long nivelId, @Param("archivadoId") Long archivadoId);
}
