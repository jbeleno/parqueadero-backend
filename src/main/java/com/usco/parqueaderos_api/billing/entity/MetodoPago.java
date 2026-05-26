package com.usco.parqueaderos_api.billing.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Metodos de pago soportados. Cubre el ecosistema colombiano formal e informal.
 * Si se requiere un metodo no listado, usar OTRO y documentar en obs externa.
 */
public enum MetodoPago {
    EFECTIVO,
    TARJETA_CREDITO,
    TARJETA_DEBITO,
    NEQUI,
    DAVIPLATA,
    PSE,
    TRANSFERENCIA,
    FLYPASS,
    QR_BANCOLOMBIA,
    OTRO;

    private static final Set<String> VALORES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    public static boolean esValido(String v) {
        return v != null && VALORES.contains(v.trim().toUpperCase());
    }

    public static String normalizar(String v) {
        return v == null ? null : v.trim().toUpperCase();
    }
}
