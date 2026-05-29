package com.usco.parqueaderos_api.device.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoDispositivo;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.entity.SubSeccion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "dispositivo")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Dispositivo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_dispositivo_id", nullable = false)
    private TipoDispositivo tipoDispositivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_seccion_id")
    private SubSeccion subSeccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id", nullable = false)
    private Parqueadero parqueadero;

    @Column(name = "url_endpoint", length = 500)
    private String urlEndpoint;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point coordenada;

    // v49 Fase 10: soft-delete uniforme (archivado_en + actor)
    @jakarta.persistence.Column(name = "archivado_en")
    private java.time.LocalDateTime archivadoEn;

    @jakarta.persistence.Column(name = "archivado_por_usuario_id")
    private Long archivadoPorUsuarioId;

    @jakarta.persistence.Column(length = 100)
    private String marca;
    @jakarta.persistence.Column(length = 100)
    private String modelo;
    @jakarta.persistence.Column(length = 100)
    private String serial;
    @jakarta.persistence.Column(name = "firmware_version", length = 50)
    private String firmwareVersion;
    @jakarta.persistence.Column(name = "ultima_lectura")
    private java.time.LocalDateTime ultimaLectura;
}
