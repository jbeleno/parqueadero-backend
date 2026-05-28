package com.usco.parqueaderos_api.convenio.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvenioDTO {
    private Long id;

    @NotNull(message = "parqueaderoId es obligatorio")
    private Long parqueaderoId;

    @NotBlank(message = "nombreComercio es obligatorio")
    @Size(max = 200, message = "nombreComercio max 200 caracteres")
    private String nombreComercio;

    @Size(max = 30, message = "nitComercio max 30 caracteres")
    private String nitComercio;

    @NotBlank(message = "tipoDescuento es obligatorio")
    @Size(max = 30, message = "tipoDescuento max 30 caracteres")
    @Pattern(regexp = "MONTO_FIJO|PORCENTAJE|MINUTOS_GRATIS",
             message = "tipoDescuento debe ser MONTO_FIJO, PORCENTAJE o MINUTOS_GRATIS")
    private String tipoDescuento;

    @PositiveOrZero(message = "valorDescuento debe ser >= 0")
    private Double valorDescuento;

    @DecimalMin(value = "0.0", message = "porcentajeDescuento >= 0")
    @DecimalMax(value = "100.0", message = "porcentajeDescuento <= 100")
    private Double porcentajeDescuento;

    @PositiveOrZero(message = "minutosGratis debe ser >= 0")
    private Integer minutosGratis;

    @PositiveOrZero(message = "montoMinimoCompra debe ser >= 0")
    private Double montoMinimoCompra;

    private LocalDateTime fechaInicioVigencia;
    private LocalDateTime fechaFinVigencia;
    private Boolean activo;
    private Long creadoPorUsuarioId;
    private String creadoPorUsuarioNombre;
    private Long desactivadoPorUsuarioId;
    private String desactivadoPorUsuarioNombre;
    private LocalDateTime desactivadoEn;
}
