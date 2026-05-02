package com.usco.parqueaderos_api.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificacionDTO {
    private String tipo;
    private String mensaje;
    private Long referenciaId;
    private Long parqueaderoId;
    private Object data;
}
