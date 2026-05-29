package com.usco.parqueaderos_api.caja.entity;

import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.user.entity.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Movimiento inmutable de una caja. Event sourcing. Cada operacion que
 * cambia el saldo del cajon queda registrada con: tipo, monto (+/-),
 * pago asociado (si aplica), motivo, usuario que ejecuto, y snapshot del
 * saldo resultante.
 *
 * Nunca se borra ni se modifica.
 */
@Entity
@Table(name = "movimiento_caja")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoCaja extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id", nullable = false)
    private Caja caja;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimientoCaja tipo;

    /** Positivo (ingreso) o negativo (retiro). */
    @Column(nullable = false)
    private Double monto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    /** Obligatorio en RETIRO/DEPOSITO/AJUSTE. */
    @Column(length = 500)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "saldo_resultante", nullable = false)
    private Double saldoResultante;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();
}
