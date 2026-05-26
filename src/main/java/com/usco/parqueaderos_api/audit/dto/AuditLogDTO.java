package com.usco.parqueaderos_api.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String tabla;
    private Long registroId;
    private String accion;
    private Long usuarioId;
    private Long empresaId;
    private String origen;
    private String motivo;
    private String valoresAntes;
    private String valoresDespues;
    private String requestId;
    private String endpoint;
    private String ip;
    private String userAgent;
    private LocalDateTime fechaHora;
}
