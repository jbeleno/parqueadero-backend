package com.usco.parqueaderos_api.parking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParqueaderoDTO {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String nombre;

    @Size(max = 300)
    private String direccion;

    @Size(max = 20)
    private String telefono;

    @DecimalMin(value = "-90.0", message = "latitud entre -90 y 90")
    @DecimalMax(value = "90.0", message = "latitud entre -90 y 90")
    private Double latitud;

    @DecimalMin(value = "-180.0", message = "longitud entre -180 y 180")
    @DecimalMax(value = "180.0", message = "longitud entre -180 y 180")
    private Double longitud;

    private Double altitud;

    @Size(max = 5, message = "horaInicio formato HH:mm")
    private String horaInicio;

    @Size(max = 5, message = "horaFin formato HH:mm")
    private String horaFin;

    @PositiveOrZero(message = "numeroPuntosParqueo >= 0")
    private Integer numeroPuntosParqueo;

    @Size(max = 50, message = "zonaHoraria max 50 caracteres")
    private String zonaHoraria;

    @PositiveOrZero(message = "tiempoGraciaMinutos >= 0")
    private Integer tiempoGraciaMinutos;

    @Size(max = 50)
    private String modoCobro;

    @NotNull(message = "La ciudad es obligatoria")
    private Long ciudadId;
    private String ciudadNombre;

    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;
    private String empresaNombre;

    @NotNull(message = "El tipo de parqueadero es obligatorio")
    private Long tipoParqueaderoId;
    private String tipoParqueaderoNombre;

    @NotNull(message = "El estado es obligatorio")
    private Long estadoId;
    private String estadoNombre;

    // Conteos de disponibilidad (read-only, calculados al serializar)
    private Long totalPuntos;
    private Long puntosDisponibles;
    private Long puntosOcupados;
    private Long puntosReservados;
}
