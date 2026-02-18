package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Información básica del parqueadero (cabecera de la configuración) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotInfoDTO {
    private Long id;
    private String name;
    private Long empresaId;
    private Long ciudadId;
    private Long tipoParqueaderoId;
    private Long estadoId;
    private Double latitud;
    private Double longitud;
    private String direccion;
    private String telefono;
    private String horaInicio;
    private String horaFin;
    private Integer numeroPisos;
    private Integer numeroPuntosParqueo;
    private String zonaHoraria;
    private Integer tiempoGraciaMinutos;
    private String modoCobro;

    // Campos de solo-lectura (pobla el GET)
    private String empresaNombre;
    private String ciudadNombre;
    private String tipoParqueaderoNombre;
    private String estadoNombre;
}
