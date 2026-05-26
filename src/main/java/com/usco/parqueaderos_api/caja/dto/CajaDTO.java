package com.usco.parqueaderos_api.caja.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CajaDTO {
    private Long id;
    private Long parqueaderoId;
    private String parqueaderoNombre;
    private Long usuarioId;
    private String usuarioCorreo;
    private String nombre;
    private Double fondoInicial;
    private Double saldoCalculado;
    private Double saldoContado;
    private Double diferencia;
    private String estado;
    private LocalDateTime abiertaEn;
    private LocalDateTime cerradaEn;
    private String observacionesApertura;
    private String observacionesCierre;
}
