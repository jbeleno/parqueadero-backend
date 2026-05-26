package com.usco.parqueaderos_api.subscription.entity;

/**
 * Tipo de movimiento sobre el saldo de una suscripcion ABONO_PREPAGO.
 * Categoriza para reportes financieros (separar carga real del cliente
 * de ajustes manuales del operador, reversos, etc).
 */
public enum TipoMovimiento {
    /** Cliente carga saldo (monto > 0). */
    ABONO,
    /** Salida del vehiculo descuenta saldo (monto < 0). */
    CONSUMO,
    /** Reverso por anulacion de ticket que descontó (devuelve saldo). */
    REVERSO,
    /** Ajuste manual del operador con justificacion (perdida, regalo, correccion). */
    AJUSTE
}
