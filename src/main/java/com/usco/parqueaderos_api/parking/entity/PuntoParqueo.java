package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoPuntoParqueo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "punto_parqueo")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class PuntoParqueo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_seccion_id", nullable = false)
    private SubSeccion subSeccion;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String acronimo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_punto_parqueo_id", nullable = false)
    private TipoPuntoParqueo tipoPuntoParqueo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon poligono;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point coordenada;

    /** Coordenadas del canvas/frontend en JSON: {"topLeft":{"x":111,"y":54},"topRight":...} */
    @Column(name = "coordenadas", columnDefinition = "TEXT")
    private String coordenadas;

    // v49 Fase 10: soft-delete uniforme (archivado_en + actor)
    @jakarta.persistence.Column(name = "archivado_en")
    private java.time.LocalDateTime archivadoEn;

    @jakarta.persistence.Column(name = "archivado_por_usuario_id")
    private Long archivadoPorUsuarioId;

    @jakarta.persistence.Column
    private Boolean reservable;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String observaciones;

    // v49 Fase 5 (completar): cols faltantes
    @jakarta.persistence.Column(name = "numero_visible", length = 20)
    private String numeroVisible;
    @jakarta.persistence.Column(name = "coordenadas_layout_x", precision = 10, scale = 2)
    private java.math.BigDecimal coordenadasLayoutX;
    @jakarta.persistence.Column(name = "coordenadas_layout_y", precision = 10, scale = 2)
    private java.math.BigDecimal coordenadasLayoutY;
    @jakarta.persistence.Column(name = "ancho_metros", precision = 5, scale = 2)
    private java.math.BigDecimal anchoMetros;
    @jakarta.persistence.Column(name = "largo_metros", precision = 5, scale = 2)
    private java.math.BigDecimal largoMetros;
    @jakarta.persistence.Column(name = "con_techo")
    private Boolean conTecho;
    @jakarta.persistence.Column(name = "con_carga_electrica")
    private Boolean conCargaElectrica;
    @jakarta.persistence.Column(name = "cerca_de_acceso")
    private Boolean cercaDeAcceso;
    @jakarta.persistence.Column(name = "para_discapacitados")
    private Boolean paraDiscapacitados;
}
