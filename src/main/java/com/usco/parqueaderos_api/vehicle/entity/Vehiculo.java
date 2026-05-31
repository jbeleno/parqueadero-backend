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

    @jakarta.persistence.Column(length = 100)
    private String marca;
    @jakarta.persistence.Column(length = 100)
    private String modelo;
    @jakarta.persistence.Column(name = "anio")
    private Integer anio;
    @jakarta.persistence.Column(name = "placa_pais", length = 2)
    private String placaPais;
    @jakarta.persistence.Column(name = "soat_vence")
    private java.time.LocalDate soatVence;
    @jakarta.persistence.Column(name = "tecnomecanica_vence")
    private java.time.LocalDate tecnomecanicaVence;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String observaciones;
    @jakarta.persistence.Column(name = "imagen_url", length = 300)
    private String imagenUrl;

    // v49 Fase 5 (completar): cols faltantes del plan
    @jakarta.persistence.Column(length = 100)
    private String linea;
    @jakarta.persistence.Column
    private Integer cilindraje;
    @jakarta.persistence.Column(name = "kilometraje_ultimo")
    private Integer kilometrajeUltimo;
    @jakarta.persistence.Column(name = "numero_chasis", length = 50)
    private String numeroChasis;
    @jakarta.persistence.Column(name = "numero_motor", length = 50)
    private String numeroMotor;
    @jakarta.persistence.Column(name = "placa_pais_id")
    private Long placaPaisId;
    @jakarta.persistence.Column(name = "tipo_servicio_id")
    private Long tipoServicioId;
    @jakarta.persistence.Column(name = "color_pintura", length = 50)
    private String colorPintura;
}
