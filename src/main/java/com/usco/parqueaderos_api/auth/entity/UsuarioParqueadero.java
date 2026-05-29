package com.usco.parqueaderos_api.auth.entity;

import com.usco.parqueaderos_api.catalog.entity.Rol;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.user.entity.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Asignacion granular de un usuario a un parqueadero con un rol especifico.
 * Permite que OPERARIO_CAJA y ADMIN_PARQUEADERO operen en N parqueaderos
 * dentro de la empresa de su ADMIN.
 *
 * Composite PK (usuario, parqueadero, rol).
 */
@Entity
@Table(name = "usuario_parqueadero",
       indexes = {
           @Index(name = "idx_usrparq_usr",  columnList = "usuario_id"),
           @Index(name = "idx_usrparq_parq", columnList = "parqueadero_id")
       })
@IdClass(UsuarioParqueadero.PK.class)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioParqueadero extends BaseEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parqueadero_id")
    private Parqueadero parqueadero;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @Column(name = "asignado_en")
    private LocalDateTime asignadoEn = LocalDateTime.now();

    @Column(name = "asignado_por_usuario_id")
    private Long asignadoPorUsuarioId;

    @Column
    private Boolean activo = true;

    @Column(name = "motivo_desasignacion", length = 500)
    private String motivoDesasignacion;

    @Column(name = "desasignado_en")
    private LocalDateTime desasignadoEn;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Long usuario;
        private Long parqueadero;
        private Long rol;
        @Override public int hashCode() { return Objects.hash(usuario, parqueadero, rol); }
        @Override public boolean equals(Object o) {
            if (!(o instanceof PK p)) return false;
            return Objects.equals(usuario, p.usuario)
                    && Objects.equals(parqueadero, p.parqueadero)
                    && Objects.equals(rol, p.rol);
        }
    }
}
