package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "sub_seccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubSeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seccion_id", nullable = false)
    private Seccion seccion;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String acronimo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon poligono;

    @Column(name = "coordenada_centro", columnDefinition = "geometry(Point,4326)")
    private Point coordenadaCentro;
}
