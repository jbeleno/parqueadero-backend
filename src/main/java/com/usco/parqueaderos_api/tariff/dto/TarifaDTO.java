package com.usco.parqueaderos_api.tariff.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotNull(message = "El valor es obligatorio")
    @Positive(message = "El valor debe ser positivo")
    private Double valor;

    @Size(max = 50)
    private String unidad;

    @PositiveOrZero(message = "minutosFraccion >= 0")
    private Integer minutosFraccion;

    @Size(max = 10, message = "fechaInicioVigencia formato YYYY-MM-DD")
    private String fechaInicioVigencia;

    @Size(max = 10, message = "fechaFinVigencia formato YYYY-MM-DD")
    private String fechaFinVigencia;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotNull(message = "El tipo de vehículo es obligatorio")
    private Long tipoVehiculoId;
    private String tipoVehiculoNombre;

    // Modelo B: gracia + minimo + cubrimiento (todos opcionales)
    @PositiveOrZero(message = "minutosGracia >= 0")
    private Integer minutosGracia;

    @PositiveOrZero(message = "valorMinimo >= 0")
    private Double valorMinimo;

    @PositiveOrZero(message = "minutosCubiertosPorMinimo >= 0")
    private Integer minutosCubiertosPorMinimo;

    // IVA opcional (parqueaderos formales)
    private Boolean aplicaIva;

    @DecimalMin(value = "0.0", message = "ivaPorcentaje >= 0")
    @DecimalMax(value = "100.0", message = "ivaPorcentaje <= 100")
    private Double ivaPorcentaje;

    /**
     * Si true, pasado minutosCubiertosPorMinimo se cobra la TARIFA NORMAL COMPLETA
     * (el minimo desaparece). Si false (default) es ADITIVO: minimo + excedente.
     */
    private Boolean valorMinimoReemplaza;

    // Suscripciones (NULL si no se ofrecen)
    @PositiveOrZero(message = "precioMensualidad >= 0")
    private Double precioMensualidad;

    @PositiveOrZero(message = "precioPaseDia >= 0")
    private Double precioPaseDia;
}
