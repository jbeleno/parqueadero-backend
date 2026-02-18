package com.usco.parqueaderos_api.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubSeccionDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String acronimo;

    private String descripcion;

    @NotNull(message = "La sección es obligatoria")
    private Long seccionId;
    private String seccionNombre;

    @NotNull(message = "El estado es obligatorio")
    private Long estadoId;
    private String estadoNombre;
}
