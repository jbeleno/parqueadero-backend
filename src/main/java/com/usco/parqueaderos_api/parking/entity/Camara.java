package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "camara")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Camara extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id", nullable = false)
    private Nivel nivel;

    /** Seccion padre (opcional). Las coordenadas nx/ny/nw/nh son relativas a esta seccion. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id")
    private Seccion seccion;

    @Column(length = 200)
    private String nombre;

    /** Coordenadas normalizadas (0..1) relativas a la seccion padre, en JSON: {"nx":0.1,"ny":0.2,"nw":0.3,"nh":0.4} */
    @Column(name = "coordenadas", columnDefinition = "TEXT")
    private String coordenadas;

    /** IDs de los puntos de parqueo asignados, en JSON: ["uuid-or-dbid", ...] */
    @Column(name = "assigned_spots", columnDefinition = "TEXT")
    private String assignedSpots;

    @Column(length = 30)
    private String color;

    /** Rol de la camara: ENTRADA, SALIDA o SEGURIDAD. Default SEGURIDAD para compatibilidad. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20)
    private TipoCamara tipo;

    /** Path relativo donde el ImageStorage guarda la ultima imagen. NULL si nunca se subio. */
    @Column(name = "imagen_path", length = 500)
    private String imagenPath;

    /** Timestamp del ultimo upload. Sirve para cache-busting en la URL. */
    @Column(name = "imagen_timestamp")
    private java.time.LocalDateTime imagenTimestamp;

    /** Content-Type de la ultima imagen (image/jpeg, image/png). */
    @Column(name = "imagen_content_type", length = 50)
    private String imagenContentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    // v49 Fase 10: soft-delete uniforme (archivado_en + actor)
    @jakarta.persistence.Column(name = "archivado_en")
    private java.time.LocalDateTime archivadoEn;

    @jakarta.persistence.Column(name = "archivado_por_usuario_id")
    private Long archivadoPorUsuarioId;

    @jakarta.persistence.Column(length = 100)
    private String marca;
    @jakarta.persistence.Column(length = 100)
    private String modelo;
    @jakarta.persistence.Column(length = 45)
    private String ip;
    @jakarta.persistence.Column(length = 17)
    private String mac;
    @jakarta.persistence.Column(length = 20)
    private String resolucion;

    // v49 Fase 5 (completar): cols faltantes del plan
    @jakarta.persistence.Column(name = "numero_serie", length = 100)
    private String numeroSerie;
    @jakarta.persistence.Column(name = "fecha_instalacion")
    private java.time.LocalDate fechaInstalacion;
    @jakarta.persistence.Column(name = "fecha_ultima_revision")
    private java.time.LocalDate fechaUltimaRevision;
    @jakarta.persistence.Column(name = "tipo_lente", length = 50)
    private String tipoLente;
    @jakarta.persistence.Column(name = "proteccion_ip", length = 10)
    private String proteccionIp;
    @jakarta.persistence.Column(name = "ubicacion_descripcion", columnDefinition = "TEXT")
    private String ubicacionDescripcion;
    @jakarta.persistence.Column(name = "url_stream_rtsp", length = 500)
    private String urlStreamRtsp;
    @jakarta.persistence.Column(name = "url_stream_http", length = 500)
    private String urlStreamHttp;
    @jakarta.persistence.Column(name = "usuario_acceso", length = 100)
    private String usuarioAcceso;
    @jakarta.persistence.Column(name = "password_acceso_cifrado", length = 500)
    private String passwordAccesoCifrado;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String observaciones;
}
