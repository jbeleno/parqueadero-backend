package com.usco.parqueaderos_api.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartamentoDTO {
    private Long id;

    @NotBlank(message = "nombre es obligatorio")
    @Size(max = 100, message = "nombre max 100 caracteres")
    private String nombre;

    @Size(max = 10, message = "identificadorNacional max 10 caracteres")
    private String identificadorNacional;

    @NotNull(message = "paisId es obligatorio")
    private Long paisId;
    private String paisNombre;
}
