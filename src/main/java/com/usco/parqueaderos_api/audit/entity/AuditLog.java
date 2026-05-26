package com.usco.parqueaderos_api.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Registro inmutable de auditoria. Append-only — NUNCA se modifica ni borra
 * desde la aplicacion. Toda operacion que cambia el estado de cualquier tabla
 * de negocio queda registrada aqui.
 *
 * Se setea automaticamente via AuditableAspect (AOP) y/o llamadas explicitas
 * a AuditService.log().
 */
@Entity
@Table(name = "audit_log",
       indexes = {
           @Index(name = "idx_audit_tabla_registro", columnList = "tabla,registro_id"),
           @Index(name = "idx_audit_usuario_fecha",  columnList = "usuario_id,fecha_hora"),
           @Index(name = "idx_audit_fecha",          columnList = "fecha_hora"),
           @Index(name = "idx_audit_accion",         columnList = "accion")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tabla de negocio afectada: "factura", "ticket", "tarifa", ... */
    @Column(nullable = false, length = 80)
    private String tabla;

    /** Id del registro afectado. NULL solo si la accion no toca un registro concreto. */
    @Column(name = "registro_id")
    private Long registroId;

    /**
     * CREATE | UPDATE | ARCHIVE | UNARCHIVE | ANULAR | DELETE_FISICO
     * | LOGIN | LOGOUT | EXPORT | CALCULO | OCR_DETECCION | JOB_TICK
     */
    @Column(nullable = false, length = 30)
    private String accion;

    /** Usuario que ejecuto. Null si fue el SISTEMA (job, listener async). */
    @Column(name = "usuario_id")
    private Long usuarioId;

    /** Empresa para filtrar audit por tenant. Calculado desde el registro. */
    @Column(name = "empresa_id")
    private Long empresaId;

    /** HTTP | JOB | LISTENER | BACKFILL | SISTEMA */
    @Column(length = 30)
    private String origen;

    /** Motivo obligatorio en cambios destructivos / criticos. NULL solo en CREATE/LOGIN. */
    @Column(length = 500)
    private String motivo;

    /** Snapshot del registro antes del cambio (null en CREATE). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_antes", columnDefinition = "jsonb")
    private String valoresAntes;

    /** Snapshot del registro despues del cambio (null en DELETE_FISICO). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_despues", columnDefinition = "jsonb")
    private String valoresDespues;

    /** TraceId del HTTP request o id sintetico del job. Para correlacion. */
    @Column(name = "request_id", length = 80)
    private String requestId;

    /** Endpoint o nombre del job (ej. "PATCH /api/tickets/42/anular"). */
    @Column(length = 200)
    private String endpoint;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;
}
