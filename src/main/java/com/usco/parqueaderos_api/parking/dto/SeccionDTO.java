package com.usco.parqueaderos_api.parking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeccionDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @Size(max = 20)
    private String acronimo;

    private String descripcion;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotNull(message = "El nivel es obligatorio")
    private Long nivelId;
    private String nivelNombre;

    @NotNull(message = "El estado es obligatorio")
    private Long estadoId;
    private String estadoNombre;
}
