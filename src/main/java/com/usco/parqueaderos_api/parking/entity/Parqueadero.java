package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoParqueadero;
import com.usco.parqueaderos_api.location.entity.Ciudad;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "parqueadero")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parqueadero {

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
    private String modoCobro; // POR_HORA, POR_FRACCION, POR_DIA, PLANA
}
