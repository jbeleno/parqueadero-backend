package com.usco.parqueaderos_api.subscription.entity;

import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Cada movimiento sobre el saldo de una Suscripcion ABONO_PREPAGO.
 * Inmutable — patron Event Sourcing para auditoria del dinero.
 *
 * - monto > 0 = abono (carga de saldo)
 * - monto < 0 = consumo (descuento por ticket)
 *
 * saldo_resultante es snapshot post-movimiento. Permite reconstruir el saldo
 * en cualquier punto del tiempo y detectar bugs (suma vs saldo_actual).
 */
@Entity
@Table(name = "movimiento_saldo")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoSaldo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suscripcion_id", nullable = false)
    private Suscripcion suscripcion;

    /** Positivo = abono, negativo = consumo. */
    @Column(nullable = false)
    private Double monto;

    /** Si el movimiento fue por consumo, link al ticket. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    /** Si el movimiento fue por abono, link al pago que lo origino. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    @Column(name = "saldo_resultante", nullable = false)
    private Double saldoResultante;

    /** Categoria para reportes (ABONO, CONSUMO, REVERSO, AJUSTE). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoMovimiento tipo;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    /** Usuario que registro el movimiento (operario que cobro, admin que ajusto, etc). */
    @Column(name = "registrado_por_usuario_id")
    private Long registradoPorUsuarioId;
}
