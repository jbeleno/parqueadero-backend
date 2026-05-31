package com.usco.parqueaderos_api.billing.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "factura")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Factura extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "valor_total", nullable = false)
    private Double valorTotal;

    @Column(length = 50)
    private String estado; // PENDIENTE, PAGADA, ANULADA

    /** Base imponible (sin IVA). Solo se setea si la tarifa aplicaba IVA. NULL = no aplica. */
    @Column(name = "base_imponible")
    private Double baseImponible;

    /** Monto de IVA cobrado. NULL = no aplica. */
    @Column(name = "iva_monto")
    private Double ivaMonto;

    /** Porcentaje de IVA usado al calcular. NULL si no aplica. */
    @Column(name = "iva_porcentaje")
    private Double ivaPorcentaje;

    /** Origen de la factura: MANUAL (POST /api/facturas), AUTO (listener al cerrar ticket), BACKFILL_xxx. */
    @Column(length = 50)
    private String origen = "MANUAL";

    /**
     * Snapshot de la resolucion DIAN usada al emitir esta factura. NULL si la
     * factura se creo antes de existir la tabla (v44 o antes).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolucion_dian_id")
    private ResolucionDian resolucionDian;

    /**
     * Usuario que emitio la factura. NULL para facturas migradas pre-v48.
     * AUTO: el operario que cerro el ticket. MANUAL: el que hizo POST /api/facturas.
     * BACKFILL_*: el SUPER_ADMIN que ejecuto el backfill.
     */
    @Column(name = "emitido_por_usuario_id")
    private Long emitidoPorUsuarioId;

    // v49 Sprint A: snapshots de historicidad para reportes inmutables.
    @Column(name = "cliente_nombre_snapshot", length = 200)
    private String clienteNombreSnapshot;
    @Column(name = "cliente_documento_snapshot", length = 50)
    private String clienteDocumentoSnapshot;
    @Column(name = "placa_snapshot", length = 20)
    private String placaSnapshot;
    @Column(name = "emitido_por_nombre_snapshot", length = 200)
    private String emitidoPorNombreSnapshot;

    @jakarta.persistence.Column(name = "fecha_vencimiento")
    private java.time.LocalDate fechaVencimiento;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String observaciones;

    // v49 Fase 5 (completar): cols faltantes del plan
    @jakarta.persistence.Column(name = "numero_factura", length = 50)
    private String numeroFactura;
    @jakarta.persistence.Column(length = 20)
    private String prefijo;
    @jakarta.persistence.Column
    private Long consecutivo;
    @jakarta.persistence.Column(length = 200)
    private String cufe;
    @jakarta.persistence.Column(precision = 14, scale = 2)
    private java.math.BigDecimal subtotal;
    @jakarta.persistence.Column(name = "descuento_aplicado", precision = 14, scale = 2)
    private java.math.BigDecimal descuentoAplicado;
    @jakarta.persistence.Column(name = "valor_total_letras", columnDefinition = "TEXT")
    private String valorTotalLetras;
    @jakarta.persistence.Column(name = "email_destinatario", length = 200)
    private String emailDestinatario;
    @jakarta.persistence.Column(name = "fecha_envio_email")
    private java.time.LocalDateTime fechaEnvioEmail;
    @jakarta.persistence.Column(name = "archivo_pdf_url", length = 500)
    private String archivoPdfUrl;
}
