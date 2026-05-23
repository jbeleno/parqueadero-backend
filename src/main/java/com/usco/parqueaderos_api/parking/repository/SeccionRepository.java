package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {
    List<Seccion> findByParqueaderoId(Long parqueaderoId);
    List<Seccion> findByNivelId(Long nivelId);
    List<Seccion> findByNivelIdAndEstadoNombreNot(Long nivelId, String estadoNombre);
    List<Seccion> findByParqueaderoEmpresaId(Long empresaId);

    /** Bulk soft-delete: archiva todas las secciones de un nivel en un solo UPDATE. */
    @Modifying
    @Query("UPDATE Seccion s SET s.estado.id = :archivadoId WHERE s.nivel.id = :nivelId")
    int archivarPorNivelId(@Param("nivelId") Long nivelId, @Param("archivadoId") Long archivadoId);
}
