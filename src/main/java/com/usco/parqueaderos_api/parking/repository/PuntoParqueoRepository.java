package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PuntoParqueoRepository extends JpaRepository<PuntoParqueo, Long> {
    List<PuntoParqueo> findBySubSeccionId(Long subSeccionId);
    List<PuntoParqueo> findBySubSeccionIdIn(List<Long> subSeccionIds);
    List<PuntoParqueo> findBySubSeccionIdInAndEstadoNombreNot(List<Long> subSeccionIds, String estadoNombre);

    @Query("SELECT COUNT(pp) FROM PuntoParqueo pp " +
           "WHERE pp.subSeccion.seccion.parqueadero.id = :parqueaderoId " +
           "AND pp.estado.nombre <> 'ARCHIVADO'")
    long countByParqueaderoId(@Param("parqueaderoId") Long parqueaderoId);
}
