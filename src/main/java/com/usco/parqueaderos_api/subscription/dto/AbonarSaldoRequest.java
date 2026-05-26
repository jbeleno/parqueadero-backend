package com.usco.parqueaderos_api.subscription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AbonarSaldoRequest {

    @NotNull
    private Long vehiculoId;

    @NotNull
    private Long parqueaderoId;

    @NotNull
    private Long tarifaId;  // Si no hay suscripcion ABONO_PREPAGO activa, se crea con esta tarifa

    @NotNull
    @Positive
    private Double monto;
}
