package com.usco.parqueaderos_api.common.entity;

import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.entity.Pago;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Valida que las entities operativas heredan correctamente {@link BaseEntity}
 * y exponen fecha_creacion + fecha_actualizacion.
 *
 * No probamos los callbacks @PrePersist/@PreUpdate aqui porque solo se
 * disparan al persistir via EntityManager real (no via constructor); eso lo
 * cubren los tests de integracion contra H2/Postgres.
 */
class BaseEntityTest {

    @Test
    @DisplayName("v49 Fase 0: las 7 entities criticas heredan BaseEntity y exponen fechaCreacion/fechaActualizacion")
    void entitiesCriticas_heredanBaseEntity() {
        LocalDateTime ahora = LocalDateTime.now();

        // Cada entity es BaseEntity y deja setear/obtener los timestamps
        assertHeredaBase(new Empresa(), ahora);
        assertHeredaBase(new Parqueadero(), ahora);
        assertHeredaBase(new Ticket(), ahora);
        assertHeredaBase(new Factura(), ahora);
        assertHeredaBase(new Pago(), ahora);
        assertHeredaBase(new Vehiculo(), ahora);
        assertHeredaBase(new Persona(), ahora);
    }

    @Test
    @DisplayName("v49 Fase 0: onCreate setea fechaCreacion = ahora y fechaActualizacion = fechaCreacion en primer insert")
    void onCreate_setea_ambos_timestamps() {
        // Subclase de prueba que expone onCreate (es protected en BaseEntity)
        TestEntity e = new TestEntity();
        assertNotNull(e);

        // Estado inicial: ambos nulos
        // (no se asume porque Lombok @Data no inicializa)
        e.setFechaCreacion(null);
        e.setFechaActualizacion(null);
        e.invokeOnCreate();

        assertNotNull(e.getFechaCreacion(), "onCreate debe setear fechaCreacion");
        assertNotNull(e.getFechaActualizacion(), "onCreate debe inicializar fechaActualizacion");
        assertEquals(e.getFechaCreacion(), e.getFechaActualizacion(),
                "En el insert inicial fechaActualizacion debe == fechaCreacion");
    }

    @Test
    @DisplayName("v49 Fase 0: onUpdate solo modifica fechaActualizacion, NO fechaCreacion")
    void onUpdate_solo_modifica_actualizacion() throws InterruptedException {
        TestEntity e = new TestEntity();
        e.invokeOnCreate();
        LocalDateTime creacionOriginal = e.getFechaCreacion();

        Thread.sleep(2); // garantizar un delta de tiempo
        e.invokeOnUpdate();

        assertEquals(creacionOriginal, e.getFechaCreacion(), "fechaCreacion debe ser inmutable tras update");
        assertTrue(e.getFechaActualizacion().isAfter(creacionOriginal),
                "fechaActualizacion debe ser posterior a la creacion");
    }

    private static void assertHeredaBase(BaseEntity entity, LocalDateTime stamp) {
        entity.setFechaCreacion(stamp);
        entity.setFechaActualizacion(stamp);
        assertEquals(stamp, entity.getFechaCreacion());
        assertEquals(stamp, entity.getFechaActualizacion());
    }

    /** Subclase de prueba que expone los callbacks protected. */
    private static class TestEntity extends BaseEntity {
        void invokeOnCreate() { onCreate(); }
        void invokeOnUpdate() { onUpdate(); }
    }
}
