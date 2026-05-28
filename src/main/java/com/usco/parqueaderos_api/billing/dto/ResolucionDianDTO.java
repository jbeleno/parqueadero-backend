package com.usco.parqueaderos_api.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolucionDianDTO {

    private Long id;

    @NotNull(message = "parqueaderoId es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotBlank(message = "numeroResolucion es obligatorio")
    private String numeroResolucion;

    @NotNull(message = "fechaResolucion es obligatoria")
    private LocalDate fechaResolucion;

    private String tipoResolucion;     // POS | FACTURA_ELECTRONICA | CONTINGENCIA
    private String modalidad;           // POS_VENTA | FACTURACION_ELECTRONICA

    private String prefijo;

    @NotNull(message = "rangoInicial es obligatorio")
    @Positive(message = "rangoInicial debe ser positivo")
    private Long rangoInicial;

    @NotNull(message = "rangoFinal es obligatorio")
    @Positive(message = "rangoFinal debe ser positivo")
    private Long rangoFinal;

    private Long consecutivoActual;

    @NotNull(message = "vigenteDesde es obligatorio")
    private LocalDate vigenteDesde;

    @NotNull(message = "vigenteHasta es obligatorio")
    private LocalDate vigenteHasta;

    @NotBlank(message = "nombre es obligatorio")
    private String nombre;

    private String descripcion;
    private String regimenTributario;

    /** Solo informativo en respuesta. Para marcar principal usar PATCH /marcar-principal. */
    private Boolean principal;

    /** Calculado: VIGENTE | VENCIDA | AGOTADA | NO_INICIADA. */
    private String estadoCalculado;

    private Long creadoPorUsuarioId;
    private LocalDateTime fechaCreacion;
    private LocalDateTime archivadaEn;
}
