package com.usco.parqueaderos_api.subscription.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MovimientoSaldoDTO {
    private Long id;
    private Long suscripcionId;
    private Double monto;
    private Long ticketId;
    private Long pagoId;
    private Double saldoResultante;
    private String motivo;
    private LocalDateTime fecha;
}
