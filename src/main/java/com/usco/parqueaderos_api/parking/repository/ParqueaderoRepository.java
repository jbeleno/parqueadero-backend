package com.usco.parqueaderos_api.parking.repository;

import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParqueaderoRepository extends JpaRepository<Parqueadero, Long> {
    List<Parqueadero> findByEmpresaId(Long empresaId);
    List<Parqueadero> findByCiudadId(Long ciudadId);

    /**
     * Parqueaderos activos accesibles para cualquier usuario:
     * - Los publicos (empresa "Público")
     * - Los de la empresa del usuario (si tiene empresa asignada)
     *
     * SUPER_ADMIN puede usar findAll directamente si quiere TODOS sin
     * importar empresa. Este endpoint sirve al USER que necesita ver
     * que parqueaderos puede usar para reservar.
     */
    @Query("SELECT p FROM Parqueadero p " +
           "WHERE (p.estado IS NULL OR LOWER(p.estado.nombre) <> 'archivado') " +
           "AND (" +
           "    LOWER(p.empresa.nombre) = 'público' " +
           " OR LOWER(p.empresa.nombre) = 'publico' " +
           " OR (:empresaIdUsuario IS NOT NULL AND p.empresa.id = :empresaIdUsuario)" +
           ")")
    List<Parqueadero> findPublicosYDeMiEmpresa(@Param("empresaIdUsuario") Long empresaIdUsuario);
}
