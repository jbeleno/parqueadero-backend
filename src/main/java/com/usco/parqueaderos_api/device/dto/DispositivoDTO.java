package com.usco.parqueaderos_api.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DispositivoDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    private String nombre;

    @Size(max = 500)
    private String urlEndpoint;

    @NotNull(message = "El tipo de dispositivo es obligatorio")
    private Long tipoDispositivoId;
    private String tipoDispositivoNombre;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotNull(message = "El estado es obligatorio")
    private Long estadoId;
    private String estadoNombre;

    private Long subSeccionId;
    private String subSeccionNombre;
}
