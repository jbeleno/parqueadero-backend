package com.usco.parqueaderos_api.ticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketDTO {

    private Long id;
    private LocalDateTime fechaHoraEntrada;
    private LocalDateTime fechaHoraSalida;

    @NotNull(message = "El vehículo es obligatorio")
    private Long vehiculoId;
    private String vehiculoPlaca;

    @NotNull(message = "El punto de parqueo es obligatorio")
    private Long puntoParqueoId;
    private String puntoParqueoNombre;

    @NotNull(message = "La tarifa es obligatoria")
    private Long tarifaId;
    private String tarifaNombre;

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;
    private String parqueaderoNombre;

    @Size(max = 50, message = "estado max 50 caracteres")
    @Pattern(regexp = "EN_CURSO|CERRADO|ANULADO",
             message = "estado debe ser EN_CURSO, CERRADO o ANULADO",
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String estado;

    @PositiveOrZero(message = "montoCalculado debe ser >= 0")
    private Double montoCalculado;

    /** Cuando la camara de salida confirmo el cruce fisico. Null si aun no. */
    private LocalDateTime fechaHoraSalidaFisica;

    /** Suscripcion que cubrio el ticket (MENSUAL/PASE_DIA/ABONO_PREPAGO). Null = cobro normal. */
    private Long suscripcionId;

    /** Trazabilidad de operador (solo lectura desde back). */
    private Long creadoPorUsuarioId;
    private String creadoPorUsuarioNombre;
    private Long cerradoPorUsuarioId;
    private String cerradoPorUsuarioNombre;
    private Long anuladoPorUsuarioId;
    private String anuladoPorUsuarioNombre;

    // v49 Sprint A: snapshots inmutables al momento del evento.
    // Si el dueño/operador/tarifa cambia despues, los reportes muestran estos
    // valores en vez del FK actual. Para registros pre-v49 vienen NULL y los
    // services de lectura caen al FK actual como fallback elegante.
    private String placaSnapshot;
    private String duenoNombreSnapshot;
    private String duenoDocumentoSnapshot;
    private String tipoVehiculoSnapshot;
    private String tarifaNombreSnapshot;
    private String puntoParqueoNombreSnapshot;
    private String operadorEntradaNombreSnapshot;
    private String operadorSalidaNombreSnapshot;
}
