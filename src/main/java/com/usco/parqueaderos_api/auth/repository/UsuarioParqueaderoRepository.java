package com.usco.parqueaderos_api.auth.repository;

import com.usco.parqueaderos_api.auth.entity.UsuarioParqueadero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioParqueaderoRepository
        extends JpaRepository<UsuarioParqueadero, UsuarioParqueadero.PK> {

    /** Parqueaderos activos asignados a un usuario, sin importar rol. */
    @Query("SELECT DISTINCT up.parqueadero.id FROM UsuarioParqueadero up " +
           "WHERE up.usuario.id = :usuarioId AND up.activo = TRUE")
    List<Long> findParqueaderoIdsByUsuario(@Param("usuarioId") Long usuarioId);

    /** Asignaciones de un usuario (para administracion). */
    List<UsuarioParqueadero> findByUsuarioId(Long usuarioId);

    /** Usuarios asignados a un parqueadero (operadores + admins parqueadero). */
    @Query("SELECT up FROM UsuarioParqueadero up WHERE up.parqueadero.id = :parqueaderoId AND up.activo = TRUE")
    List<UsuarioParqueadero> findActivasByParqueadero(@Param("parqueaderoId") Long parqueaderoId);
}
