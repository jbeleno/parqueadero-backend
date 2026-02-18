package com.usco.parqueaderos_api.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {
    private String tipo;       // TICKET_CREADO, TICKET_CERRADO, RESERVA_CREADA
    private String mensaje;
    private Long referenciaId; // ID del ticket/reserva
    private Long parqueaderoId;
}
