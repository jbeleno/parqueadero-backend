package com.usco.parqueaderos_api.ticket.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketDTO {

    private Long id;
    private LocalDateTime fechaHoraEntrada;
    private LocalDateTime fechaHoraSalida;

    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;

    @NotNull(message = "El punto de parqueo es obligatorio")
    private Long puntoParqueoId;
    private String puntoParqueoNombre;

    @NotNull(message = "La tarifa es obligatoria")
    private Long tarifaId;
    private String tarifaNombre;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    private String estado;
    private Double montoCalculado;
}
