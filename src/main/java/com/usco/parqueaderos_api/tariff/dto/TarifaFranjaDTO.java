package com.usco.parqueaderos_api.tariff.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaFranjaDTO {
    private Long id;
    private Long tarifaId;
    private String nombre;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Double valor;
    private Boolean soloFinesDeSemana;
    private Boolean activa;
}
