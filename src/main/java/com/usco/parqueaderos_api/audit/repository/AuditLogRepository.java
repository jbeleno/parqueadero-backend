package com.usco.parqueaderos_api.audit.repository;

import com.usco.parqueaderos_api.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTablaAndRegistroIdOrderByFechaHoraDesc(String tabla, Long registroId);

    Page<AuditLog> findByUsuarioIdOrderByFechaHoraDesc(Long usuarioId, Pageable pageable);

    Page<AuditLog> findByEmpresaIdOrderByFechaHoraDesc(Long empresaId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:tabla IS NULL OR a.tabla = :tabla) AND " +
           "(:usuarioId IS NULL OR a.usuarioId = :usuarioId) AND " +
           "(:empresaId IS NULL OR a.empresaId = :empresaId) AND " +
           "(:accion IS NULL OR a.accion = :accion) AND " +
           "(:desde IS NULL OR a.fechaHora >= :desde) AND " +
           "(:hasta IS NULL OR a.fechaHora <= :hasta) " +
           "ORDER BY a.fechaHora DESC")
    Page<AuditLog> buscar(
            @Param("tabla") String tabla,
            @Param("usuarioId") Long usuarioId,
            @Param("empresaId") Long empresaId,
            @Param("accion") String accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);
}
