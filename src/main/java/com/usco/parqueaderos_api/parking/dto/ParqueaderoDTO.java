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
public class ParqueaderoDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombre;

    @Size(max = 300)
    private String direccion;

    @Size(max = 20)
    private String telefono;

    private Double latitud;
    private Double longitud;
    private Double altitud;
    private String horaInicio;
    private String horaFin;
    private Integer numeroPuntosParqueo;
    private String zonaHoraria;
    private Integer tiempoGraciaMinutos;

    @Size(max = 50)
    private String modoCobro;

    @NotNull(message = "La ciudad es obligatoria")
    private Long ciudadId;
    private String ciudadNombre;

    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;
    private String empresaNombre;

    @NotNull(message = "El tipo de parqueadero es obligatorio")
    private Long tipoParqueaderoId;
    private String tipoParqueaderoNombre;

    @NotNull(message = "El estado es obligatorio")
    private Long estadoId;
    private String estadoNombre;
}
