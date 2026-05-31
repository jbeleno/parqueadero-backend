package com.usco.parqueaderos_api.billing.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FacturaDTO {

    private Long id;
    private LocalDateTime fechaHora;

    @NotNull(message = "El ticket es obligatorio")
    private Long ticketId;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;

    @PositiveOrZero(message = "valorTotal debe ser >= 0")
    private Double valorTotal;

    @Size(max = 50, message = "estado max 50 caracteres")
    @Pattern(regexp = "PENDIENTE|PAGADA|ANULADA|VENCIDA",
             message = "estado debe ser PENDIENTE, PAGADA, ANULADA o VENCIDA",
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String estado;

    @PositiveOrZero(message = "baseImponible debe ser >= 0")
    private Double baseImponible;

    @PositiveOrZero(message = "ivaMonto debe ser >= 0")
    private Double ivaMonto;

    @DecimalMin(value = "0.0", message = "ivaPorcentaje >= 0")
    @DecimalMax(value = "100.0", message = "ivaPorcentaje <= 100")
    private Double ivaPorcentaje;

    /** MANUAL, AUTO, BACKFILL_<timestamp>. Solo lectura desde el back. */
    @Size(max = 50, message = "origen max 50 caracteres")
    private String origen;

    /** ID de la resolucion DIAN usada al emitir esta factura (snapshot). Null si no aplica. */
    private Long resolucionDianId;
    private String resolucionDianNumero;

    /** Usuario que emitio la factura. Solo lectura. */
    private Long emitidoPorUsuarioId;
    private String emitidoPorUsuarioNombre;

    // v49 Sprint A: snapshots inmutables al momento de emision
    private String clienteNombreSnapshot;
    private String clienteDocumentoSnapshot;
    private String placaSnapshot;
    private String emitidoPorNombreSnapshot;

    // v49 Fase 7: cross-field validations
    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message = "Si hay ivaMonto, baseImponible + ivaMonto debe igualar valorTotal (tolerancia 0.01)")
    public boolean isIvaConsistente() {
        if (valorTotal == null) return true;
        if (ivaMonto == null || ivaMonto == 0.0) return true;
        if (baseImponible == null) return false;
        double suma = baseImponible + ivaMonto;
        return Math.abs(suma - valorTotal) <= 0.01;
    }
}
