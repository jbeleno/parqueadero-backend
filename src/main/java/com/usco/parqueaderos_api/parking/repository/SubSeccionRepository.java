package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.SubSeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubSeccionRepository extends JpaRepository<SubSeccion, Long> {
    List<SubSeccion> findBySeccionId(Long seccionId);
    List<SubSeccion> findBySeccionIdIn(List<Long> seccionIds);
    List<SubSeccion> findBySeccionIdInAndEstadoNombreNot(List<Long> seccionIds, String estadoNombre);
    List<SubSeccion> findBySeccionParqueaderoEmpresaId(Long empresaId);

    /** Bulk soft-delete: archiva todas las subsecciones de las secciones del nivel. */
    @Modifying
    @Query("UPDATE SubSeccion ss SET ss.estado.id = :archivadoId WHERE ss.seccion.nivel.id = :nivelId")
    int archivarPorNivelId(@Param("nivelId") Long nivelId, @Param("archivadoId") Long archivadoId);
}
