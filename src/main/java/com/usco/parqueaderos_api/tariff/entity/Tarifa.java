package com.usco.parqueaderos_api.tariff.entity;

import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tarifa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_vehiculo_id", nullable = false)
    private TipoVehiculo tipoVehiculo;

    @Column(nullable = false)
    private Double valor;

    @Column(length = 50)
    private String unidad; // POR_MINUTO, POR_HORA, POR_FRACCION, POR_DIA, PLANA

    @Column(name = "minutos_fraccion")
    private Integer minutosFraccion;

    @Column(name = "fecha_inicio_vigencia")
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    /** Minutos gratuitos al inicio de cada ticket. Antes de este umbral, monto=0. */
    @Column(name = "minutos_gracia")
    private Integer minutosGracia = 0;

    /** Cobro plano (fee de entrada) que cubre los primeros minutos_cubiertos_por_minimo. */
    @Column(name = "valor_minimo")
    private Double valorMinimo = 0.0;

    /**
     * Cuantos minutos cubre el valor_minimo. Despues de este umbral, se suma
     * la tarifa normal (por hora/fraccion/dia) sobre los minutos excedentes.
     */
    @Column(name = "minutos_cubiertos_por_minimo")
    private Integer minutosCubiertosPorMinimo = 0;

    /** Precio de una mensualidad (Suscripcion tipo MENSUAL). NULL si no se ofrece. */
    @Column(name = "precio_mensualidad")
    private Double precioMensualidad;

    /** Precio de un pase de dia (Suscripcion tipo PASE_DIA). NULL si no se ofrece. */
    @Column(name = "precio_pase_dia")
    private Double precioPaseDia;

    /**
     * Si TRUE: el valor minimo es la tarifa de estadia corta. Pasado
     * minutos_cubiertos_por_minimo, se cobra la TARIFA NORMAL COMPLETA en su
     * lugar (no se suma al minimo). Modo "umbral / reemplazo".
     *
     * Si FALSE (default): el minimo + tarifa_normal(excedente) se suman.
     * Modelo aditivo clasico (Modelo B).
     *
     * Ejemplo con valorMinimo=2000, cubre=5, valor=3000/h:
     *   modo aditivo (false): 60min -> 2000 + ceil(55/60)*3000 = 5000
     *   modo reemplazo (true): 60min -> ceil(60/60)*3000 = 3000  (el minimo desaparece)
     */
    @Column(name = "valor_minimo_reemplaza")
    private Boolean valorMinimoReemplaza = false;

    /** Si TRUE el monto cobrado incluye IVA (se calcula y desagrega en la factura). */
    @Column(name = "aplica_iva")
    private Boolean aplicaIva = false;

    /** Porcentaje de IVA (Colombia: 19.0 estandar). Solo se usa si aplicaIva=true. */
    @Column(name = "iva_porcentaje")
    private Double ivaPorcentaje = 0.0;
}
