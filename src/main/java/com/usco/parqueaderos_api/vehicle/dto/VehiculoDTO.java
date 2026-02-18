package com.usco.parqueaderos_api.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehiculoDTO {

    private Long id;

    @NotBlank(message = "La placa es obligatoria")
    @Size(max = 20)
    private String placa;

    @Size(max = 50)
    private String color;

    @NotNull(message = "El tipo de vehículo es obligatorio")
    private Long tipoVehiculoId;
    private String tipoVehiculoNombre;

    @NotNull(message = "La persona es obligatoria")
    private Long personaId;
    private String personaNombre;
    private String personaDocumento;
}
