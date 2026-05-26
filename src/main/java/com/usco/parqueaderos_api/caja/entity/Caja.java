package com.usco.parqueaderos_api.caja.entity;

import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.user.entity.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Turno de operacion de una caja fisica. Una caja se abre con fondo inicial,
 * recibe pagos en EFECTIVO automaticamente, permite retiros/depositos
 * manuales, y se cierra con arqueo.
 *
 * Restriccion: un usuario solo puede tener 1 caja ABIERTA a la vez (unique
 * index parcial). Para operar en otro parqueadero debe cerrar la anterior.
 */
@Entity
@Table(name = "caja")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(length = 100)
    private String nombre;

    @Column(name = "fondo_inicial", nullable = false)
    private Double fondoInicial;

    @Column(name = "saldo_calculado", nullable = false)
    private Double saldoCalculado;

    @Column(name = "saldo_contado")
    private Double saldoContado;

    @Column
    private Double diferencia;

    /** ABIERTA | CERRADA */
    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "abierta_en", nullable = false)
    private LocalDateTime abiertaEn = LocalDateTime.now();

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    @Column(name = "observaciones_apertura", columnDefinition = "TEXT")
    private String observacionesApertura;

    @Column(name = "observaciones_cierre", columnDefinition = "TEXT")
    private String observacionesCierre;
}
