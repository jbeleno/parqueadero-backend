package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "camino")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Camino {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nivel_id", nullable = false)
    private Nivel nivel;

    @Column(length = 50)
    private String tipo; // polyline, etc.

    /** Coordenadas del canvas/frontend en JSON: [{"x":473,"y":45}, ...] */
    @Column(name = "coordenadas", columnDefinition = "TEXT")
    private String coordenadas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;
}
