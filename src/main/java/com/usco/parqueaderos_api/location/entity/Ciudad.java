package com.usco.parqueaderos_api.location.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.usco.parqueaderos_api.common.entity.BaseEntity;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ciudad")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Ciudad extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departamento_id", nullable = false)
    private Departamento departamento;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "identificador_departamental", length = 10)
    private String identificadorDepartamental;
}
