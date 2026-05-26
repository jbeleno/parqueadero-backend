package com.usco.parqueaderos_api.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ResumenFacturacionDTO {
    private LocalDate desde;
    private LocalDate hasta;
    private Long parqueaderoId;
    private String agruparPor;          // dia | semana | mes | tipo_vehiculo | operador | fuente
    // Totales del rango
    private Double totalFacturado;
    private Double totalPagado;
    private Double totalPendiente;
    private Long cantidadFacturas;
    private Long cantidadTickets;
    private Long vehiculosUnicos;
    private Double visitasPromedioPorVehiculo;
    // Buckets agrupados
    private List<BucketFacturacionDTO> buckets;
}
