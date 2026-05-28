package com.usco.parqueaderos_api.billing.entity;

import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resolucion DIAN registrada por el admin del parqueadero. Cada parqueadero
 * puede tener varias resoluciones a lo largo del tiempo (al renovarse) pero
 * solo UNA con principal=true a la vez (indice unico parcial en BD).
 *
 * Al emitir una factura se referencia el id (snapshot historico) — si despues
 * cambia la principal, las facturas viejas siguen apuntando a la original.
 */
@Entity
@Table(name = "resolucion_dian",
       indexes = {
           @Index(name = "idx_resol_parq",      columnList = "parqueadero_id"),
           @Index(name = "idx_resol_principal", columnList = "parqueadero_id,principal")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolucionDian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    /** Numero asignado por la DIAN. Ej: "18764000000123". */
    @Column(name = "numero_resolucion", nullable = false, length = 50)
    private String numeroResolucion;

    /** Fecha en que la DIAN emitio la resolucion. */
    @Column(name = "fecha_resolucion", nullable = false)
    private LocalDate fechaResolucion;

    /** POS | FACTURA_ELECTRONICA | CONTINGENCIA. */
    @Column(name = "tipo_resolucion", length = 30)
    private String tipoResolucion;

    /** POS_VENTA | FACTURACION_ELECTRONICA. */
    @Column(length = 30)
    private String modalidad;

    /** Prefijo de la numeracion. Ej: "POS", "FE", o "" si no aplica. */
    @Column(length = 20)
    private String prefijo;

    /** Numero inicial del rango autorizado. */
    @Column(name = "rango_inicial", nullable = false)
    private Long rangoInicial;

    /** Numero final del rango autorizado. */
    @Column(name = "rango_final", nullable = false)
    private Long rangoFinal;

    /** Ultimo numero emitido. Auto-incrementa al generar factura. */
    @Column(name = "consecutivo_actual")
    private Long consecutivoActual = 0L;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta", nullable = false)
    private LocalDate vigenteHasta;

    /** Alias amigable para el admin. Ej: "POS Sede Centro 2026". */
    @Column(nullable = false, length = 150)
    private String nombre;

    /** Texto libre (notas operativas, regimen, etc). */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Pie del recibo asociado (opcional, overrides Parqueadero.pieRecibo). */
    @Column(name = "regimen_tributario", length = 100)
    private String regimenTributario;

    /**
     * Marca cual es la resolucion default del parqueadero (la que el sistema
     * usa al emitir factura). Solo UNA por parqueadero (UNIQUE INDEX parcial).
     */
    @Column(nullable = false)
    private Boolean principal = false;

    @Column(name = "creado_por_usuario_id")
    private Long creadoPorUsuarioId;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    /** Soft-delete. NULL = activa/disponible. !NULL = archivada. */
    @Column(name = "archivada_en")
    private LocalDateTime archivadaEn;

    @Column(name = "motivo_archivado", length = 300)
    private String motivoArchivado;

    @Column(name = "archivado_por_usuario_id")
    private Long archivadoPorUsuarioId;
}
