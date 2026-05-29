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
}
