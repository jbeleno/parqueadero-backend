package com.usco.parqueaderos_api.convenio.entity;

import com.usco.parqueaderos_api.ticket.entity.Ticket;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Comprobante de compra del comercio aplicado a un ticket. Inmutable
 * (event sourcing). El descuento se calcula al cerrar el ticket.
 *
 * Una sola ValidacionCompra por ticket (regla de negocio comun;
 * apilar varias se podria habilitar despues si el operador lo requiere).
 */
@Entity
@Table(name = "validacion_compra")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionCompra extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convenio_id", nullable = false)
    private Convenio convenio;

    /** Monto declarado de la compra (para verificar monto minimo). */
    @Column(name = "monto_compra", nullable = false)
    private Double montoCompra;

    /** Folio/recibo externo del comercio. */
    @Column(name = "folio_externo", length = 100)
    private String folioExterno;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDateTime fechaAplicacion = LocalDateTime.now();

    /** Descuento efectivamente aplicado en el cobro del ticket. */
    @Column(name = "descuento_aplicado")
    private Double descuentoAplicado;

    /** Usuario (operario o admin) que registro el comprobante. */
    @Column(name = "registrado_por_usuario_id")
    private Long registradoPorUsuarioId;
}
