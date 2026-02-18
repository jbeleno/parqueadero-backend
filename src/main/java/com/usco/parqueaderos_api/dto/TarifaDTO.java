package com.usco.parqueaderos_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaDTO {
    private Long id;
    private String nombre;
    private Double valor;
    private String unidad;
    private Integer minutosFraccion;
    private String fechaInicioVigencia;
    private String fechaFinVigencia;
    private Long parqueaderoId;
    private String parqueaderoNombre;
    private Long tipoVehiculoId;
    private String tipoVehiculoNombre;
}
