package com.usco.parqueaderos_api.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeudaVehiculoDTO {
    private Long vehiculoId;
    private String placa;
    private Double totalAdeudado;
    private Integer cantidadFacturas;
    private List<FacturaDTO> facturasPendientes;
}
