package com.usco.parqueaderos_api.subscription.dto;

import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CrearSuscripcionRequest {

    @NotNull
    private Long vehiculoId;

    @NotNull
    private Long parqueaderoId;

    @NotNull
    private Long tarifaId;

    @NotNull
    private TipoSuscripcion tipo;

    /**
     * Monto pagado por la suscripcion. Para MENSUAL/PASE_DIA debe coincidir
     * con el precio configurado en la Tarifa. Para ABONO_PREPAGO es el saldo
     * inicial cargado.
     */
    @NotNull
    @Positive
    private Double montoPagado;
}
