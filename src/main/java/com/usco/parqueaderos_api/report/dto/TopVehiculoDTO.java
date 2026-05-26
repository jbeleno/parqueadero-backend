package com.usco.parqueaderos_api.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopVehiculoDTO {
    private Long vehiculoId;
    private String placa;
    private Long cantidadTickets;
    private Double totalFacturado;
}
