package com.usco.parqueaderos_api.tariff.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Franja horaria que sobreescribe el valor base de una tarifa.
 * Permite modelar tarifas nocturnas, peak hours, fin de semana, etc.
 *
 * Si la hora de ENTRADA del ticket cae dentro de horaInicio..horaFin,
 * se usa este valor en lugar del valor base de la Tarifa.
 *
 * Para franjas que cruzan medianoche (ej. 22:00 -> 06:00) horaFin < horaInicio.
 */
@Entity
@Table(name = "tarifa_franja")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TarifaFranja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarifa_id", nullable = false)
    private Tarifa tarifa;

    @Column(nullable = false, length = 50)
    private String nombre; // NOCTURNA, PEAK, FIN_DE_SEMANA, etc.

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** Valor que reemplaza al valor base cuando la franja aplica. */
    @Column(nullable = false)
    private Double valor;

    /** Solo aplica fines de semana (sab/dom). */
    @Column(name = "solo_fines_de_semana", nullable = false)
    private Boolean soloFinesDeSemana = false;

    /** Activa/inactiva sin borrar el registro. */
    @Column(nullable = false)
    private Boolean activa = true;
}
