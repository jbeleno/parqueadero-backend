package com.usco.parqueaderos_api.reservation.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reserva")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Reserva extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_parqueo_id")
    private PuntoParqueo puntoParqueo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(length = 50)
    private String estado; // PENDIENTE, CONFIRMADA, CANCELADA, EXPIRADA

    @jakarta.persistence.Column(name = "canal_origen", length = 50)
    private String canalOrigen;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String notas;
    @jakarta.persistence.Column(name = "confirmada_en")
    private java.time.LocalDateTime confirmadaEn;
    @jakarta.persistence.Column(name = "cancelada_en")
    private java.time.LocalDateTime canceladaEn;
    @jakarta.persistence.Column(name = "cancelada_por_usuario_id")
    private Long canceladaPorUsuarioId;
    @jakarta.persistence.Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    // v49 Fase 5 (completar): cols faltantes del plan
    @jakarta.persistence.Column(name = "monto_estimado", precision = 12, scale = 2)
    private java.math.BigDecimal montoEstimado;
    @jakarta.persistence.Column(name = "monto_anticipo_pagado", precision = 12, scale = 2)
    private java.math.BigDecimal montoAnticipoPagado;
    @jakarta.persistence.Column(name = "pago_anticipo_id")
    private Long pagoAnticipoId;
    @jakarta.persistence.Column(name = "codigo_confirmacion", length = 50)
    private String codigoConfirmacion;
    @jakarta.persistence.Column(name = "hora_real_llegada")
    private java.time.LocalDateTime horaRealLlegada;
    @jakarta.persistence.Column(name = "monto_penalizacion_no_show", precision = 12, scale = 2)
    private java.math.BigDecimal montoPenalizacionNoShow;
    @jakarta.persistence.Column(name = "canal_origen_id")
    private Long canalOrigenId;
    @jakarta.persistence.Column(name = "notificaciones_enabled")
    private Boolean notificacionesEnabled;
    @jakarta.persistence.Column(name = "recordatorios_enviados")
    private Integer recordatoriosEnviados;
}
