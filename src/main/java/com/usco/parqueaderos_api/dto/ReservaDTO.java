package com.usco.parqueaderos_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReservaDTO {
    private Long id;
    private String codigo;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private Long usuarioId;
    private String usuarioNombre;
    private Long puntoParqueoId;
    private String puntoParqueoNombre;
    private Long estadoId;
    private String estadoNombre;
    private Double monto;
}
