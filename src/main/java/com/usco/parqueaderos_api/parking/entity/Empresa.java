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

    // v49 Fase 5 (completar): cols faltantes del plan
    @jakarta.persistence.Column(name = "digito_verificacion", length = 2)
    private String digitoVerificacion;
    @jakarta.persistence.Column(name = "razon_social", length = 300)
    private String razonSocial;
    @jakarta.persistence.Column(name = "nombre_comercial", length = 200)
    private String nombreComercial;
    @jakarta.persistence.Column(name = "representante_legal_persona_id")
    private Long representanteLegalPersonaId;
    @jakarta.persistence.Column(name = "fecha_constitucion")
    private java.time.LocalDate fechaConstitucion;
    @jakarta.persistence.Column(name = "zona_horaria_id")
    private Long zonaHorariaId;
    @jakarta.persistence.Column(name = "email_facturacion", length = 200)
    private String emailFacturacion;
}
