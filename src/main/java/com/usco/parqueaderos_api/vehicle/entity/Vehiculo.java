package com.usco.parqueaderos_api.vehicle.entity;

import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import com.usco.parqueaderos_api.user.entity.Persona;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculo")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Vehiculo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @Column(nullable = false, unique = true, length = 20)
    private String placa;

    @Column(length = 50)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_vehiculo_id", nullable = false)
    private TipoVehiculo tipoVehiculo;

    /** Soft-delete: false = archivado, no aparece en sugerencias. Default true. */
    @Column
    private Boolean activo = true;

    /** Marca de tiempo del archivado. Null si esta activo. */
    @Column(name = "archivado_en")
    private LocalDateTime archivadoEn;

    /** Marcado por OCR cuando crea vehiculo sin persona registrada. */
    @Column(name = "es_visitante")
    private Boolean esVisitante = false;

    /** Ultima actividad (ticket o reserva). Para limpieza de visitantes inactivos. */
    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;
}
