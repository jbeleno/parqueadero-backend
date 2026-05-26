package com.usco.parqueaderos_api.convenio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionCompraDTO {
    private Long id;
    private Long ticketId;
    private Long convenioId;
    private String nombreComercio;
    private Double montoCompra;
    private String folioExterno;
    private LocalDateTime fechaAplicacion;
    private Double descuentoAplicado;
}
