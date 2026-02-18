package com.usco.parqueaderos_api.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FacturaDTO {
    private Long id;
    private String numero;
    private LocalDate fecha;
    private Long ticketId;
    private String ticketCodigo;
    private Long usuarioId;
    private String usuarioNombre;
    private Double subtotal;
    private Double impuesto;
    private Double total;
    private Long estadoId;
    private String estadoNombre;
}
