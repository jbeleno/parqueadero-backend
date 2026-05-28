package com.usco.parqueaderos_api.billing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(nullable = false)
    private Double monto;

    @Column(length = 50)
    private String metodo; // EFECTIVO, TARJETA, APP

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(length = 50)
    private String estado; // PENDIENTE, COMPLETADO, FALLIDO, ANULADO

    /** Motivo de anulacion / reverso (chargeback, error operativo, etc). */
    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    @Column(name = "anulado_por_usuario_id")
    private Long anuladoPorUsuarioId;

    /** Usuario que registro el pago (quien apreto "Cobrar"). Critico para cuadre de caja. */
    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;
}
