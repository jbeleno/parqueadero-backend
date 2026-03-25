package com.usco.parqueaderos_api.location.dto;

import lombok.Data;

@Data
public class DepartamentoDTO {
    private Long id;
    private String nombre;
    private String identificadorNacional;
    private Long paisId;
    private String paisNombre;
}
