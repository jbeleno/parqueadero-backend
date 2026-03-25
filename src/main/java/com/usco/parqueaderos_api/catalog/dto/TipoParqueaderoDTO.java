package com.usco.parqueaderos_api.catalog.dto;

import lombok.Data;

@Data
public class TipoParqueaderoDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Long estadoId;
    private String estadoNombre;
}
