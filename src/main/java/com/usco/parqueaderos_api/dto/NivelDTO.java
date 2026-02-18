package com.usco.parqueaderos_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NivelDTO {
    private Long id;
    private String nombre;
    private Long parqueaderoId;
    private String parqueaderoNombre;
    private Long estadoId;
    private String estadoNombre;
}
