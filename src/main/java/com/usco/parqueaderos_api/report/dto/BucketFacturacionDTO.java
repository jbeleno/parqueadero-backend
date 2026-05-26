package com.usco.parqueaderos_api.report.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BucketFacturacionDTO {
    /** Periodo: yyyy-MM-dd (dia), yyyy-WW (semana), yyyy-MM (mes), o etiqueta categorica */
    private String periodo;
    private Double facturado;
    private Double pagado;
    private Double pendiente;
    private Long cantidadFacturas;
    private Long cantidadTickets;
    private Long vehiculosUnicos;
}
