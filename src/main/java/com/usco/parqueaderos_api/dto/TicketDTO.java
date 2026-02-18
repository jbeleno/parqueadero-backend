package com.usco.parqueaderos_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TicketDTO {
    private Long id;
    private String codigo;
    private LocalDateTime fechaHoraEntrada;
    private LocalDateTime fechaHoraSalida;
    private Long vehiculoId;
    private String vehiculoPlaca;
    private Long puntoParqueoId;
    private String puntoParqueoNombre;
    private Long tarifaId;
    private String tarifaNombre;
    private Long estadoId;
    private String estadoNombre;
    private Double montoTotal;
}
