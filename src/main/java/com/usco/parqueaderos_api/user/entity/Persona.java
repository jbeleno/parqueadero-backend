package com.usco.parqueaderos_api.user.entity;

import com.usco.parqueaderos_api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "persona")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Persona extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(length = 20)
    private String telefono;

    @Column(name = "tipo_documento", length = 50)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 50)
    private String numeroDocumento;

    // v49 Fase 10: soft-delete uniforme (archivado_en + actor)
    @jakarta.persistence.Column(name = "archivado_en")
    private java.time.LocalDateTime archivadoEn;

    @jakarta.persistence.Column(name = "archivado_por_usuario_id")
    private Long archivadoPorUsuarioId;

    @jakarta.persistence.Column(name = "segundo_nombre", length = 100)
    private String segundoNombre;
    @jakarta.persistence.Column(name = "segundo_apellido", length = 100)
    private String segundoApellido;
    @jakarta.persistence.Column(length = 200)
    private String correo;
    @jakarta.persistence.Column(name = "fecha_nacimiento")
    private java.time.LocalDate fechaNacimiento;
    @jakarta.persistence.Column(name = "genero_id")
    private Long generoId;
    @jakarta.persistence.Column(name = "tipo_documento_id")
    private Long tipoDocumentoId;
    @jakarta.persistence.Column(length = 300)
    private String direccion;
    @jakarta.persistence.Column(name = "ciudad_id")
    private Long ciudadId;
}
