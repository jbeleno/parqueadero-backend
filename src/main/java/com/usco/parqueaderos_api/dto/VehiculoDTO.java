package com.usco.parqueaderos_api.dto;

import lombok.Data;

@Data
public class VehiculoDTO {
    private Long id;
    private String placa;
    private String marca;
    private String modelo;
    private String color;
    private Long tipoVehiculoId;
    private String tipoVehiculoNombre;
    private Long personaId;
    private String personaNombre;
    private String personaDocumento;
}
