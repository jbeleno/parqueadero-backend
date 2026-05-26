package com.usco.parqueaderos_api.subscription.entity;

public enum EstadoSuscripcion {
    /** Creada pero pago no confirmado, no da derechos todavia. */
    PENDIENTE,
    /** Vigente, da derechos. */
    ACTIVA,
    /** Paso fecha_fin. Marca via job @Scheduled diario. */
    VENCIDA,
    /** Solo ABONO_PREPAGO: saldo_restante = 0. */
    AGOTADA,
    /** ADMIN la cancelo (con o sin reembolso). */
    CANCELADA
}
