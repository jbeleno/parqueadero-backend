package com.usco.parqueaderos_api.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usco.parqueaderos_api.audit.context.AuditContext;
import com.usco.parqueaderos_api.audit.context.AuditContextHolder;
import com.usco.parqueaderos_api.audit.entity.AuditLog;
import com.usco.parqueaderos_api.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio central de auditoria. Cualquier modulo puede llamarlo para registrar
 * una accion. El AuditableAspect lo invoca automaticamente para metodos
 * anotados con @Auditable. Tambien hay llamadas manuales en flujos especiales
 * (calculos, deteccion OCR, jobs).
 *
 * REQUIRES_NEW: cada registro de auditoria persiste en su propia transaccion.
 * Si la accion principal hace rollback, el audit sigue persistido (queremos
 * saber tambien los intentos fallidos cuando aplique).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules() // jsr310 etc
            .disable(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String tabla, Long registroId, String accion,
                    Object antes, Object despues, Long empresaId) {
        log(tabla, registroId, accion, antes, despues, empresaId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String tabla, Long registroId, String accion,
                    Object antes, Object despues, Long empresaId, String motivoOverride) {
        AuditContext ctx = AuditContextHolder.get();
        AuditLog row = AuditLog.builder()
                .tabla(tabla)
                .registroId(registroId)
                .accion(accion)
                .usuarioId(ctx != null ? ctx.getUsuarioId() : null)
                .empresaId(empresaId)
                .origen(ctx != null && ctx.getOrigen() != null ? ctx.getOrigen() : "HTTP")
                .motivo(motivoOverride != null
                        ? motivoOverride
                        : (ctx != null ? ctx.getMotivo() : null))
                .valoresAntes(toJson(antes))
                .valoresDespues(toJson(despues))
                .requestId(ctx != null ? ctx.getRequestId() : null)
                .endpoint(ctx != null ? ctx.getEndpoint() : null)
                .ip(ctx != null ? ctx.getIp() : null)
                .userAgent(ctx != null ? ctx.getUserAgent() : null)
                .fechaHora(LocalDateTime.now())
                .build();
        repository.save(row);
    }

    /** Variante minima para eventos sin antes/despues (login, calculo, deteccion). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvento(String tabla, Long registroId, String accion,
                          Long empresaId, String motivo) {
        log(tabla, registroId, accion, null, null, empresaId, motivo);
    }

    private String toJson(Object o) {
        if (o == null) return null;
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            log.warn("Audit: no se pudo serializar a JSON ({}): {}",
                    o.getClass().getSimpleName(), e.getMessage());
            return "{\"_serialization_error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
