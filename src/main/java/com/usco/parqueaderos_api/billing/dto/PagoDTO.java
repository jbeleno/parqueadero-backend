package com.usco.parqueaderos_api.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PagoDTO {

    private Long id;
    private LocalDateTime fechaHora;

    @NotNull(message = "La factura es obligatoria")
    private Long facturaId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    private Double monto;

    @NotBlank(message = "metodo es obligatorio")
    @Size(max = 50, message = "metodo max 50 caracteres")
    private String metodo;

    @Size(max = 50, message = "estado max 50 caracteres")
    @Pattern(regexp = "PENDIENTE|COMPLETADO|FALLIDO|ANULADO",
             message = "estado debe ser PENDIENTE, COMPLETADO, FALLIDO o ANULADO",
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String estado;

    /** Usuario que registro el cobro. Solo lectura desde el back. */
    private Long creadoPorUsuarioId;
    private String creadoPorUsuarioNombre;
    /** Usuario que anulo el pago (NULL si nunca se anulo). */
    private Long anuladoPorUsuarioId;
    private String anuladoPorUsuarioNombre;
}
