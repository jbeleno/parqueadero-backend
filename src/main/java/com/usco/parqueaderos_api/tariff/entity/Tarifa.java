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
    private String unidad; // POR_HORA, POR_FRACCION, POR_DIA, PLANA

    @Column(name = "minutos_fraccion")
    private Integer minutosFraccion;

    @Column(name = "fecha_inicio_vigencia")
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    /** Minutos gratuitos al inicio de cada ticket. Antes de este umbral, monto=0. */
    @Column(name = "minutos_gracia", nullable = false)
    private Integer minutosGracia = 0;

    /** Cobro plano (fee de entrada) que cubre los primeros minutos_cubiertos_por_minimo. */
    @Column(name = "valor_minimo", nullable = false)
    private Double valorMinimo = 0.0;

    /**
     * Cuantos minutos cubre el valor_minimo. Despues de este umbral, se suma
     * la tarifa normal (por hora/fraccion/dia) sobre los minutos excedentes.
     */
    @Column(name = "minutos_cubiertos_por_minimo", nullable = false)
    private Integer minutosCubiertosPorMinimo = 0;

    /** Precio de una mensualidad (Suscripcion tipo MENSUAL). NULL si no se ofrece. */
    @Column(name = "precio_mensualidad")
    private Double precioMensualidad;

    /** Precio de un pase de dia (Suscripcion tipo PASE_DIA). NULL si no se ofrece. */
    @Column(name = "precio_pase_dia")
    private Double precioPaseDia;
}
