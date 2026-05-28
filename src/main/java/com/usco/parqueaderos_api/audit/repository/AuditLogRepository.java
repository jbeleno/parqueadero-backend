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

    /**
     * Busqueda con filtros opcionales. NATIVA + CAST porque PostgreSQL no infiere
     * el tipo de placeholders NULL en JPQL puro y rompe con
     * "could not determine data type of parameter".
     * Mismo fix aplicado en ReportesSpecs (v45).
     */
    @Query(value =
           "SELECT * FROM audit_log " +
           "WHERE (CAST(:tabla     AS VARCHAR)   IS NULL OR tabla       = :tabla) " +
           "  AND (CAST(:usuarioId AS BIGINT)    IS NULL OR usuario_id  = :usuarioId) " +
           "  AND (CAST(:empresaId AS BIGINT)    IS NULL OR empresa_id  = :empresaId) " +
           "  AND (CAST(:accion    AS VARCHAR)   IS NULL OR accion      = :accion) " +
           "  AND (CAST(:desde     AS TIMESTAMP) IS NULL OR fecha_hora >= :desde) " +
           "  AND (CAST(:hasta     AS TIMESTAMP) IS NULL OR fecha_hora <= :hasta) " +
           "ORDER BY fecha_hora DESC",
           countQuery =
           "SELECT COUNT(*) FROM audit_log " +
           "WHERE (CAST(:tabla     AS VARCHAR)   IS NULL OR tabla       = :tabla) " +
           "  AND (CAST(:usuarioId AS BIGINT)    IS NULL OR usuario_id  = :usuarioId) " +
           "  AND (CAST(:empresaId AS BIGINT)    IS NULL OR empresa_id  = :empresaId) " +
           "  AND (CAST(:accion    AS VARCHAR)   IS NULL OR accion      = :accion) " +
           "  AND (CAST(:desde     AS TIMESTAMP) IS NULL OR fecha_hora >= :desde) " +
           "  AND (CAST(:hasta     AS TIMESTAMP) IS NULL OR fecha_hora <= :hasta)",
           nativeQuery = true)
    Page<AuditLog> buscar(
            @Param("tabla") String tabla,
            @Param("usuarioId") Long usuarioId,
            @Param("empresaId") Long empresaId,
            @Param("accion") String accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);
}
