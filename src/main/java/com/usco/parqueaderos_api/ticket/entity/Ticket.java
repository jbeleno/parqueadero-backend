package com.usco.parqueaderos_api.ticket.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.tariff.entity.Tarifa;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_parqueo_id", nullable = false)
    private PuntoParqueo puntoParqueo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id", nullable = false)
    private Tarifa tarifa;

    @Column(name = "fecha_hora_entrada", nullable = false)
    private LocalDateTime fechaHoraEntrada;

    @Column(name = "fecha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    /**
     * Cuando la camara SALIDA confirma fisicamente que el vehiculo cruzo.
     * Puede ser igual o posterior a fechaHoraSalida si el ticket se cerro
     * manualmente antes de que el carro pasara por la camara. Solo se setea
     * la primera vez que la camara lo ve; reentradas en la ventana de
     * cooldown no la sobreescriben.
     */
    @Column(name = "fecha_hora_salida_fisica")
    private LocalDateTime fechaHoraSalidaFisica;

    @Column(length = 50)
    private String estado; // EN_CURSO, CERRADO, ANULADO

    @Column(name = "monto_calculado")
    private Double montoCalculado;

    /**
     * Suscripcion que cubrio el ticket (MENSUAL / PASE_DIA / ABONO_PREPAGO).
     * NULL si fue cobro normal con tarifa. Util para reportes de fuente de
     * ingresos: cuanto entro por suscripciones vs tickets esporadicos.
     */
    @Column(name = "suscripcion_id")
    private Long suscripcionId;

    /** Motivo justificado de anulacion del ticket (obligatorio al anular). */
    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    @Column(name = "anulado_por_usuario_id")
    private Long anuladoPorUsuarioId;

    /** Usuario que registro la entrada del vehiculo (operario o admin). */
    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;

    /** Usuario que registro la salida o cierre del ticket. NULL si aun esta EN_CURSO. */
    @Column(name = "cerrado_por_usuario_id")
    private Long cerradoPorUsuarioId;

    /**
     * Snapshot del valor/unidad/Modelo B de la tarifa al momento de la entrada.
     * Si el operador cambia la tarifa entre entrada y salida, el cobro respeta
     * el valor de entrada (anti-fraude / consistencia regulatoria).
     */
    @Column(name = "tarifa_valor_snapshot")
    private Double tarifaValorSnapshot;
    @Column(name = "tarifa_unidad_snapshot", length = 50)
    private String tarifaUnidadSnapshot;
    @Column(name = "tarifa_minimo_snapshot")
    private Double tarifaMinimoSnapshot;
    @Column(name = "tarifa_gracia_snapshot")
    private Integer tarifaGraciaSnapshot;
    @Column(name = "tarifa_cubre_snapshot")
    private Integer tarifaCubreSnapshot;

    // v49 Sprint A: snapshots de historicidad — preservan datos legibles del
    // momento del evento, evitan que reportes muestren al dueño/operador
    // actual cuando algo cambia tras la creacion del ticket.
    @Column(name = "placa_snapshot", length = 20)
    private String placaSnapshot;
    @Column(name = "dueno_nombre_snapshot", length = 200)
    private String duenoNombreSnapshot;
    @Column(name = "dueno_documento_snapshot", length = 50)
    private String duenoDocumentoSnapshot;
    @Column(name = "tipo_vehiculo_snapshot", length = 100)
    private String tipoVehiculoSnapshot;
    @Column(name = "tarifa_nombre_snapshot", length = 100)
    private String tarifaNombreSnapshot;
    @Column(name = "punto_parqueo_nombre_snapshot", length = 100)
    private String puntoParqueoNombreSnapshot;
    @Column(name = "operador_entrada_nombre_snapshot", length = 200)
    private String operadorEntradaNombreSnapshot;
    @Column(name = "operador_salida_nombre_snapshot", length = 200)
    private String operadorSalidaNombreSnapshot;
}
