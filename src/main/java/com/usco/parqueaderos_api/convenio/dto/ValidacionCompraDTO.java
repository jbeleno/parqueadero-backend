package com.usco.parqueaderos_api.convenio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionCompraDTO {
    private Long id;

    @NotNull(message = "ticketId es obligatorio")
    private Long ticketId;

    @NotNull(message = "convenioId es obligatorio")
    private Long convenioId;

    @Size(max = 200, message = "nombreComercio max 200 caracteres")
    private String nombreComercio;

    @NotNull(message = "montoCompra es obligatorio")
    @Positive(message = "montoCompra debe ser > 0")
    private Double montoCompra;

    @Size(max = 100, message = "folioExterno max 100 caracteres")
    private String folioExterno;

    private LocalDateTime fechaAplicacion;

    @PositiveOrZero(message = "descuentoAplicado debe ser >= 0")
    private Double descuentoAplicado;

    private Long registradoPorUsuarioId;
    private String registradoPorUsuarioNombre;
}
