package com.usco.parqueaderos_api.subscription.dto;

import com.usco.parqueaderos_api.subscription.entity.EstadoSuscripcion;
import com.usco.parqueaderos_api.subscription.entity.TipoSuscripcion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SuscripcionDTO {
    private Long id;
    private Long vehiculoId;
    private String vehiculoPlaca;
    private Long parqueaderoId;
    private String parqueaderoNombre;
    private Long tarifaId;
    private TipoSuscripcion tipo;
    private EstadoSuscripcion estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Double montoPagado;
    private Double saldoRestante;
    private LocalDateTime fechaCreacion;

    /** Id del punto reservado por esta suscripcion. Null si no reserva ninguno. */
    private Long puntoParqueoReservadoId;
    /** Nombre del punto reservado (read-only). */
    private String puntoParqueoReservadoNombre;
}
