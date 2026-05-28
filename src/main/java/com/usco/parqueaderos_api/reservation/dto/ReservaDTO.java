package com.usco.parqueaderos_api.reservation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservaDTO {

    private Long id;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime fechaHoraInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fechaHoraFin;

    @NotNull(message = "El usuario es obligatorio")
    private Long usuarioId;
    private String usuarioNombre;

    private Long puntoParqueoId;
    private String puntoParqueoNombre;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;

    @Size(max = 50, message = "estado max 50 caracteres")
    @Pattern(regexp = "PENDIENTE|CONFIRMADA|CANCELADA|EXPIRADA|COMPLETADA",
             message = "estado debe ser PENDIENTE, CONFIRMADA, CANCELADA, EXPIRADA o COMPLETADA",
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String estado;

    /** Cross-field: fin debe ser posterior a inicio. */
    @JsonIgnore
    @AssertTrue(message = "fechaHoraFin debe ser posterior a fechaHoraInicio")
    public boolean isRangoFechaValido() {
        if (fechaHoraInicio == null || fechaHoraFin == null) return true;
        return fechaHoraFin.isAfter(fechaHoraInicio);
    }
}
