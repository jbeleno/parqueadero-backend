package com.usco.parqueaderos_api.dto;

import lombok.Data;

@Data
public class SubSeccionDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private Long seccionId;
    private String seccionNombre;
}
