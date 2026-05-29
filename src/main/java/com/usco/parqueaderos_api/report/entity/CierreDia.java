package com.usco.parqueaderos_api.report.entity;

import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cierre de caja del dia por parqueadero. Snapshot generado por el job nocturno
 * a las 23:55. Permite reconciliacion del efectivo vs sistema.
 */
@Entity
@Table(name = "cierre_dia",
       uniqueConstraints = @UniqueConstraint(columnNames = {"parqueadero_id", "fecha"}))
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class CierreDia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "tickets_cerrados")
    private Integer ticketsCerrados;

    @Column(name = "total_cobrado")
    private Double totalCobrado;

    @Column(name = "total_efectivo")
    private Double totalEfectivo;

    @Column(name = "total_tarjeta")
    private Double totalTarjeta;

    @Column(name = "total_otros")
    private Double totalOtros;

    @Column(name = "facturas_emitidas")
    private Integer facturasEmitidas;

    @Column(name = "total_pendiente")
    private Double totalPendiente;

    @Column(name = "tickets_anulados")
    private Integer ticketsAnulados;

    @Column(name = "generado_en")
    private LocalDateTime generadoEn = LocalDateTime.now();
}
