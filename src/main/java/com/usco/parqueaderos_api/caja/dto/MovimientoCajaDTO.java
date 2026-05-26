package com.usco.parqueaderos_api.caja.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoCajaDTO {
    private Long id;
    private Long cajaId;
    private String tipo;
    private Double monto;
    private Long pagoId;
    private String motivo;
    private Long usuarioId;
    private Double saldoResultante;
    private LocalDateTime fechaHora;
}
