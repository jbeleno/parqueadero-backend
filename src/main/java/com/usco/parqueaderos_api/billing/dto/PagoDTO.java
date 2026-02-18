package com.usco.parqueaderos_api.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PagoDTO {

    private Long id;
    private LocalDateTime fechaHora;

    @NotNull(message = "La factura es obligatoria")
    private Long facturaId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private Double monto;

    private String metodo;
    private String estado;
}
