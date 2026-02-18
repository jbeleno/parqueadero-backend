package com.usco.parqueaderos_api.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FacturaDTO {

    private Long id;
    private LocalDateTime fechaHora;

    @NotNull(message = "El ticket es obligatorio")
    private Long ticketId;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;

    private Double valorTotal;
    private String estado;
}
