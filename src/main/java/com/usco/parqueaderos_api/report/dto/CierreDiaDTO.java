package com.usco.parqueaderos_api.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CierreDiaDTO {
    private Long id;
    private Long parqueaderoId;
    private String parqueaderoNombre;
    private LocalDate fecha;
    private Integer ticketsCerrados;
    private Double totalCobrado;
    private Double totalEfectivo;
    private Double totalTarjeta;
    private Double totalOtros;
    private Integer facturasEmitidas;
    private Double totalPendiente;
    private Integer ticketsAnulados;
    private LocalDateTime generadoEn;
}
