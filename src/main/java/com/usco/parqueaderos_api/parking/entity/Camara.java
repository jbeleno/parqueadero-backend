package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "camara")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Camara {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;
}
