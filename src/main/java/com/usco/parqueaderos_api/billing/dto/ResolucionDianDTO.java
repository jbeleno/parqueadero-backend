package com.usco.parqueaderos_api.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
    @Size(max = 50, message = "numeroResolucion max 50 caracteres")
    private String numeroResolucion;

    @NotNull(message = "fechaResolucion es obligatoria")
    private LocalDate fechaResolucion;

    @Size(max = 30, message = "tipoResolucion max 30 caracteres")
    @Pattern(regexp = "POS|FACTURA_ELECTRONICA|CONTINGENCIA",
             message = "tipoResolucion debe ser POS, FACTURA_ELECTRONICA o CONTINGENCIA")
    private String tipoResolucion;

    @Size(max = 30, message = "modalidad max 30 caracteres")
    @Pattern(regexp = "POS_VENTA|FACTURACION_ELECTRONICA",
             message = "modalidad debe ser POS_VENTA o FACTURACION_ELECTRONICA")
    private String modalidad;

    @Size(max = 20, message = "prefijo max 20 caracteres")
    private String prefijo;

    @NotNull(message = "rangoInicial es obligatorio")
    @Positive(message = "rangoInicial debe ser positivo")
    private Long rangoInicial;

    @NotNull(message = "rangoFinal es obligatorio")
    @Positive(message = "rangoFinal debe ser positivo")
    private Long rangoFinal;

    @PositiveOrZero(message = "consecutivoActual debe ser >= 0")
    private Long consecutivoActual;

    @NotNull(message = "vigenteDesde es obligatorio")
    private LocalDate vigenteDesde;

    @NotNull(message = "vigenteHasta es obligatorio")
    private LocalDate vigenteHasta;

    @NotBlank(message = "nombre es obligatorio")
    @Size(max = 150, message = "nombre max 150 caracteres")
    private String nombre;

    @Size(max = 5000, message = "descripcion max 5000 caracteres")
    private String descripcion;

    @Size(max = 100, message = "regimenTributario max 100 caracteres")
    private String regimenTributario;

    /** Solo informativo en respuesta. Para marcar principal usar PATCH /marcar-principal. */
    private Boolean principal;

    /** Calculado: VIGENTE | VENCIDA | AGOTADA | NO_INICIADA. */
    private String estadoCalculado;

    private Long creadoPorUsuarioId;
    private String creadoPorUsuarioNombre;
    private LocalDateTime fechaCreacion;
    private LocalDateTime archivadaEn;

    /** Cross-field: rango final debe ser >= rango inicial. */
    @JsonIgnore
    @AssertTrue(message = "rangoFinal debe ser >= rangoInicial")
    public boolean isRangoValido() {
        if (rangoInicial == null || rangoFinal == null) return true;
        return rangoFinal >= rangoInicial;
    }

    /** Cross-field: vigencia coherente. */
    @JsonIgnore
    @AssertTrue(message = "vigenteHasta debe ser >= vigenteDesde")
    public boolean isVigenciaValida() {
        if (vigenteDesde == null || vigenteHasta == null) return true;
        return !vigenteHasta.isBefore(vigenteDesde);
    }
}
