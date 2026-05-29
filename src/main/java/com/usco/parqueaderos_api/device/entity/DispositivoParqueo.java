package com.usco.parqueaderos_api.device.entity;

import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dispositivo_parqueo")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoParqueo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispositivo_id", nullable = false)
    private Dispositivo dispositivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_parqueo_id", nullable = false)
    private PuntoParqueo puntoParqueo;
}
