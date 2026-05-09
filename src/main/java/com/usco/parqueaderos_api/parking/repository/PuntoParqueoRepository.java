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

    List<PuntoParqueo> findBySubSeccionSeccionParqueaderoEmpresaId(Long empresaId);

    /** IDs de puntos OCUPADOS (con ticket EN_CURSO) entre los dados. Para batch del estadoOperativo. */
    @Query("SELECT t.puntoParqueo.id FROM Ticket t " +
           "WHERE t.puntoParqueo.id IN :puntoIds AND t.estado = 'EN_CURSO'")
    java.util.Set<Long> idsOcupadosEntre(@Param("puntoIds") java.util.Collection<Long> puntoIds);

    /** IDs de puntos RESERVADOS activos entre los dados. */
    @Query("SELECT r.puntoParqueo.id FROM Reserva r " +
           "WHERE r.puntoParqueo.id IN :puntoIds " +
           "AND r.estado IN ('PENDIENTE', 'CONFIRMADA') " +
           "AND :ahora BETWEEN r.fechaHoraInicio AND r.fechaHoraFin")
    java.util.Set<Long> idsReservadosEntre(@Param("puntoIds") java.util.Collection<Long> puntoIds,
                                            @Param("ahora") java.time.LocalDateTime ahora);
}
