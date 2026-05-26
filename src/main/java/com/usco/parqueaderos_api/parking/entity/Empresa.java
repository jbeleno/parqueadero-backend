package com.usco.parqueaderos_api.parking.entity;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empresa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

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
    @Column(name = "modo_operacion", length = 20, nullable = false)
    private String modoOperacion = "INFORMAL"; // FORMAL | INFORMAL

    /** NIT o documento del responsable. Obligatorio si modoOperacion=FORMAL. */
    @Column(length = 30)
    private String nit;
}
