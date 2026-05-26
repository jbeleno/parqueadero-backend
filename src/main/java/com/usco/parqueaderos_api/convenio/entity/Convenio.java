package com.usco.parqueaderos_api.convenio.entity;

import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Convenio comercial con un local/comercio cercano al parqueadero.
 * Permite que el cliente del comercio reciba un descuento (o gratuidad)
 * sobre el cobro de su ticket presentando un comprobante de compra.
 *
 * Modelos soportados:
 * - MONTO_FIJO: descuenta valorDescuento del cobro.
 * - PORCENTAJE: descuenta porcentajeDescuento % del cobro.
 * - MINUTOS_GRATIS: cubre minutosGratis al inicio del ticket (similar a gracia ad-hoc).
 */
@Entity
@Table(name = "convenio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Convenio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @Column(nullable = false, length = 200)
    private String nombreComercio;

    /** NIT u otro id externo del comercio (opcional). */
    @Column(length = 30)
    private String nitComercio;

    /** MONTO_FIJO | PORCENTAJE | MINUTOS_GRATIS */
    @Column(nullable = false, length = 30)
    private String tipoDescuento;

    /** Solo si tipoDescuento = MONTO_FIJO. */
    @Column(name = "valor_descuento")
    private Double valorDescuento;

    /** Solo si tipoDescuento = PORCENTAJE. */
    @Column(name = "porcentaje_descuento")
    private Double porcentajeDescuento;

    /** Solo si tipoDescuento = MINUTOS_GRATIS. */
    @Column(name = "minutos_gratis")
    private Integer minutosGratis;

    /** Compra minima requerida para que el descuento aplique. NULL = sin minimo. */
    @Column(name = "monto_minimo_compra")
    private Double montoMinimoCompra;

    @Column(name = "fecha_inicio_vigencia")
    private LocalDateTime fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private LocalDateTime fechaFinVigencia;

    @Column(nullable = false)
    private Boolean activo = true;
}
