package com.usco.parqueaderos_api.reservation.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reserva")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Reserva extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_parqueo_id")
    private PuntoParqueo puntoParqueo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(length = 50)
    private String estado; // PENDIENTE, CONFIRMADA, CANCELADA, EXPIRADA

    @jakarta.persistence.Column(name = "canal_origen", length = 50)
    private String canalOrigen;
    @jakarta.persistence.Column(columnDefinition = "TEXT")
    private String notas;
    @jakarta.persistence.Column(name = "confirmada_en")
    private java.time.LocalDateTime confirmadaEn;
    @jakarta.persistence.Column(name = "cancelada_en")
    private java.time.LocalDateTime canceladaEn;
    @jakarta.persistence.Column(name = "cancelada_por_usuario_id")
    private Long canceladaPorUsuarioId;
    @jakarta.persistence.Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;
}
