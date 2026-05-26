package com.usco.parqueaderos_api.tariff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    private Integer minutosFraccion;
    private String fechaInicioVigencia;
    private String fechaFinVigencia;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotNull(message = "El tipo de vehículo es obligatorio")
    private Long tipoVehiculoId;
    private String tipoVehiculoNombre;

    // Modelo B: gracia + minimo + cubrimiento (todos opcionales)
    private Integer minutosGracia;
    private Double valorMinimo;
    private Integer minutosCubiertosPorMinimo;

    // IVA opcional (parqueaderos formales)
    private Boolean aplicaIva;
    private Double ivaPorcentaje;

    /**
     * Si true, pasado minutosCubiertosPorMinimo se cobra la TARIFA NORMAL COMPLETA
     * (el minimo desaparece). Si false (default) es ADITIVO: minimo + excedente.
     */
    private Boolean valorMinimoReemplaza;

    // Suscripciones (NULL si no se ofrecen)
    private Double precioMensualidad;
    private Double precioPaseDia;
}
