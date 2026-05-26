package com.usco.parqueaderos_api.audit.context;

import lombok.Builder;
import lombok.Data;

/**
 * Contexto de auditoria de un request o ejecucion (job). ThreadLocal.
 *
 * Se llena en el filtro HTTP (AuditContextFilter) con datos del JWT + headers.
 * Para jobs: AuditContextManager.runAs(...) lo setea manualmente.
 */
@Data
@Builder
public class AuditContext {
    /** Usuario que ejecuto la operacion. Null si fue SISTEMA. */
    private Long usuarioId;
    /** Origen: HTTP | JOB | LISTENER | BACKFILL | SISTEMA */
    private String origen;
    /** Motivo provisto por el cliente via header X-Motivo-Operacion. */
    private String motivo;
    /** ID de correlacion (trace id). */
    private String requestId;
    /** Endpoint HTTP o nombre del job. */
    private String endpoint;
    private String ip;
    private String userAgent;
}
