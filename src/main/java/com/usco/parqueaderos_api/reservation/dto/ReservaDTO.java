package com.usco.parqueaderos_api.reservation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservaDTO {

    private Long id;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaHoraInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fechaHoraFin;

    @NotNull(message = "El usuario es obligatorio")
    private Long usuarioId;
    private String usuarioNombre;

    private Long puntoParqueoId;
    private String puntoParqueoNombre;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;

    private String estado;
}
