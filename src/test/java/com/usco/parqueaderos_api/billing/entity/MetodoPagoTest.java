package com.usco.parqueaderos_api.billing.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetodoPagoTest {

    @Test
    @DisplayName("Valores conocidos pasan validacion")
    void valores_validos() {
        assertTrue(MetodoPago.esValido("EFECTIVO"));
        assertTrue(MetodoPago.esValido("NEQUI"));
        assertTrue(MetodoPago.esValido("daviplata")); // case insensitive
        assertTrue(MetodoPago.esValido("  TARJETA_CREDITO  "));
        assertTrue(MetodoPago.esValido("OTRO"));
    }

    @Test
    @DisplayName("Valores desconocidos son rechazados")
    void valores_invalidos() {
        assertFalse(MetodoPago.esValido("BITCOIN"));
        assertFalse(MetodoPago.esValido("APP")); // ya no se permite, era ambiguo
        assertFalse(MetodoPago.esValido(""));
        assertFalse(MetodoPago.esValido(null));
    }

    @Test
    @DisplayName("Normaliza a uppercase y trim")
    void normaliza_uppercase() {
        assertEquals("NEQUI", MetodoPago.normalizar(" nequi "));
        assertEquals("EFECTIVO", MetodoPago.normalizar("efectivo"));
        assertNull(MetodoPago.normalizar(null));
    }
}
