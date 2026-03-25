package com.usco.parqueaderos_api.location.dto;

import lombok.Data;

@Data
public class CiudadDTO {
    private Long id;
    private String nombre;
    private String identificadorDepartamental;
    private Long departamentoId;
    private String departamentoNombre;
}
