package com.usco.parqueaderos_api.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ComparativaPeriodoDTO {
    private LocalDate periodoActualDesde;
    private LocalDate periodoActualHasta;
    private LocalDate periodoAnteriorDesde;
    private LocalDate periodoAnteriorHasta;

    private Double facturadoActual;
    private Double facturadoAnterior;
    private Double deltaAbsoluto;
    private Double deltaPorcentual;

    private Long ticketsActual;
    private Long ticketsAnterior;
    private Double deltaTicketsPorcentual;
}
