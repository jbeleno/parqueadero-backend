package com.usco.parqueaderos_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParqueaderoDTO {
    private Long id;
    private String nombre;
    private String direccion;
    private String telefono;
    private Double latitud;
    private Double longitud;
    private String horaInicio;
    private String horaFin;
    private Integer numeroPuntosParqueo;
    private Integer tiempoGraciaMinutos;
    private String modoCobro;
    private Long ciudadId;
    private String ciudadNombre;
    private Long empresaId;
    private String empresaNombre;
    private Long tipoParqueaderoId;
    private String tipoParqueaderoNombre;
    private Long estadoId;
    private String estadoNombre;
}
