package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "seccion")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Seccion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id", nullable = false)
    private Nivel nivel;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String acronimo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon poligono;

    @Column(name = "coordenada_centro", columnDefinition = "geometry(Point,4326)")
    private Point coordenadaCentro;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Coordenadas del canvas/frontend en formato JSON: [{"x":69,"y":32}, ...] */
    @Column(name = "coordenadas", columnDefinition = "TEXT")
    private String coordenadas;

    // v49 Fase 10: soft-delete uniforme (archivado_en + actor)
    @jakarta.persistence.Column(name = "archivado_en")
    private java.time.LocalDateTime archivadoEn;

    @jakarta.persistence.Column(name = "archivado_por_usuario_id")
    private Long archivadoPorUsuarioId;
}
