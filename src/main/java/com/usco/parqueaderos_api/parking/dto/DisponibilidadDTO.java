package com.usco.parqueaderos_api.parking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadDTO {
    private Long parqueaderoId;
    private long total;
    private long disponibles;
    private long ocupados;
    private long reservados;
}
