package com.usco.parqueaderos_api.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PuntoParqueoDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String acronimo;

    private String descripcion;

    @NotNull(message = "La sub-sección es obligatoria")
    private Long subSeccionId;
    private String subSeccionNombre;

    @NotNull(message = "El tipo de punto de parqueo es obligatorio")
    private Long tipoPuntoParqueoId;
    private String tipoPuntoParqueoNombre;

    @NotNull(message = "El estado es obligatorio")
    private Long estadoId;
    private String estadoNombre;

    private String ubicacionWkt;
}
