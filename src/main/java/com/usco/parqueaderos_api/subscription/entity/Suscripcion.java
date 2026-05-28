package com.usco.parqueaderos_api.subscription.entity;

import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Suscripcion de un vehiculo a un parqueadero. Cubre tres modelos:
 * - MENSUAL: paga $X, entra/sale libre durante 30 dias.
 * - PASE_DIA: paga $X, 24 horas de uso libre desde la compra.
 * - ABONO_PREPAGO: carga $X de saldo, se descuenta por cada salida.
 *
 * Solo puede haber UNA suscripcion ACTIVA del mismo tipo por (vehiculo, parqueadero).
 * Garantizado por indice unico parcial en BD.
 */
@Entity
@Table(name = "suscripcion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Suscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id", nullable = false)
    private Tarifa tarifa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSuscripcion tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSuscripcion estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    /** Monto pagado al comprar la suscripcion. Congelado: si despues cambia el precio, este no se afecta. */
    @Column(name = "monto_pagado", nullable = false)
    private Double montoPagado;

    /** Solo para ABONO_PREPAGO. Resto = abonos - consumos. NULL para otros tipos. */
    @Column(name = "saldo_restante")
    private Double saldoRestante;

    /**
     * Punto fijo asignado al duenio de la suscripcion (oficinas, conjuntos).
     * NULL = sin reserva (cliente entra a cualquier punto disponible).
     * Si esta seteado, ese punto queda bloqueado para uso externo durante la vigencia.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_parqueo_reservado_id")
    private com.usco.parqueaderos_api.parking.entity.PuntoParqueo puntoParqueoReservado;

    /** Optimistic lock para evitar race en descuento de saldo. */
    @Version
    @Column
    private Long version = 0L;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Usuario que creo la suscripcion (admin/admin_parq que cobro y emitio). */
    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;

    /** Usuario que la cancelo (NULL si esta vigente o vencio sola). */
    @Column(name = "cancelado_por_usuario_id")
    private Long canceladoPorUsuarioId;

    @Column(name = "cancelado_en")
    private LocalDateTime canceladoEn;
}
