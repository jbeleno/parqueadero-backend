package com.usco.parqueaderos_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PagoDTO {
    private Long id;
    private String codigo;
    private LocalDateTime fechaHora;
    private Long facturaId;
    private String facturaNumero;
    private Double monto;
    private String metodoPago;
    private String referencia;
    private Long estadoId;
    private String estadoNombre;
}
