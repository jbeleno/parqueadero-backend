package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empresa")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Empresa extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_id", nullable = false)
    private Estado estado;

    /**
     * Modo de operacion: FORMAL (DIAN, IVA, factura electronica)
     * o INFORMAL (parqueadero de barrio, sin IVA, recibo simple).
     * Default INFORMAL para compatibilidad con datos existentes.
     */
    @Column(name = "modo_operacion", length = 20)
    private String modoOperacion = "INFORMAL"; // FORMAL | INFORMAL

    /** NIT o documento del responsable. Obligatorio si modoOperacion=FORMAL. */
    @Column(length = 30)
    private String nit;

    // v49 Fase 10: soft-delete uniforme (archivado_en + actor)
    @jakarta.persistence.Column(name = "archivado_en")
    private java.time.LocalDateTime archivadoEn;

    @jakarta.persistence.Column(name = "archivado_por_usuario_id")
    private Long archivadoPorUsuarioId;

    @jakarta.persistence.Column(name = "tipo_documento_id")
    private Long tipoDocumentoId;
    @jakarta.persistence.Column(name = "regimen_tributario_id")
    private Long regimenTributarioId;
    @jakarta.persistence.Column(name = "moneda_id")
    private Long monedaId;
    @jakarta.persistence.Column(length = 300)
    private String direccion;
    @jakarta.persistence.Column(name = "ciudad_id")
    private Long ciudadId;
    @jakarta.persistence.Column(name = "correo_contacto", length = 200)
    private String correoContacto;
    @jakarta.persistence.Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;
    @jakarta.persistence.Column(name = "sitio_web", length = 300)
    private String sitioWeb;
    @jakarta.persistence.Column(name = "logo_url", length = 300)
    private String logoUrl;
}
