package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoParqueadero;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import com.usco.parqueaderos_api.location.entity.Ciudad;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "parqueadero")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Parqueadero extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_parqueadero_id", nullable = false)
    private TipoParqueadero tipoParqueadero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 300)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    private Double latitud;

    private Double longitud;

    private Double altitud;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "numero_puntos_parqueo")
    private Integer numeroPuntosParqueo;

    @Column(name = "zona_horaria", length = 50)
    private String zonaHoraria;

    @Column(name = "tiempo_gracia_minutos")
    private Integer tiempoGraciaMinutos;

    @Column(name = "modo_cobro", length = 50)
    private String modoCobro; // POR_MINUTO, POR_HORA, POR_FRACCION, POR_DIA, PLANA

    /**
     * Tarifa maxima legal por minuto (ej. Bogota tope regulado).
     * Si una tarifa configurada excede este tope, el calculador la trunca.
     * NULL = sin tope.
     */
    @Column(name = "tarifa_maxima_por_minuto")
    private Double tarifaMaximaPorMinuto;

    // ─── Configuracion de RECIBO (editable, auditada) ───────────────────

    /**
     * Resolucion DIAN (texto libre, multilinea). Aparece en el pie del recibo.
     * Ej: "Resolucion DIAN No. 18764000000123 del 2025-08-15. Numeracion del
     *      00001 al 50000. Vigencia 24 meses. Regimen Simplificado."
     */
    @Column(name = "resolucion_dian", columnDefinition = "TEXT")
    private String resolucionDian;

    /**
     * Texto libre adicional al pie (mensaje legal, "no responsable de IVA",
     * "regimen comun", politicas, etc).
     */
    @Column(name = "pie_recibo", columnDefinition = "TEXT")
    private String pieRecibo;

    /**
     * Texto libre al encabezado del recibo (slogan, contacto extra, etc).
     */
    @Column(name = "encabezado_recibo", columnDefinition = "TEXT")
    private String encabezadoRecibo;

    /** Mensaje de regimen tributario en una linea. Ej: "Responsable de IVA". */
    @Column(name = "regimen_tributario", length = 100)
    private String regimenTributario;

    /** URL del logo (img/png pequeño). Solo para impresoras grafica. */
    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    // v49 Fase 10: soft-delete uniforme (archivado_en + actor)
    @jakarta.persistence.Column(name = "archivado_en")
    private java.time.LocalDateTime archivadoEn;

    @jakarta.persistence.Column(name = "archivado_por_usuario_id")
    private Long archivadoPorUsuarioId;
}
