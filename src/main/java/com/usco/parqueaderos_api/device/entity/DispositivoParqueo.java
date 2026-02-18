package com.usco.parqueaderos_api.device.entity;

import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dispositivo_parqueo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoParqueo {

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
