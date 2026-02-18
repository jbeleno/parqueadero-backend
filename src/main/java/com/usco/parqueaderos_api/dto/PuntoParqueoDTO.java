package com.usco.parqueaderos_api.dto;

import lombok.Data;

@Data
public class PuntoParqueoDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private Long subSeccionId;
    private String subSeccionNombre;
    private Long tipoPuntoParqueoId;
    private String tipoPuntoParqueoNombre;
    private Long estadoId;
    private String estadoNombre;
    private String ubicacionWkt; // Para enviar/recibir como WKT
}
