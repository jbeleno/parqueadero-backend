package com.usco.parqueaderos_api.subscription.entity;

public enum TipoSuscripcion {
    /** El vehiculo paga $X y puede entrar/salir libre durante 30 dias. */
    MENSUAL,
    /** El vehiculo paga $X y tiene 24 horas de uso libre desde la compra. */
    PASE_DIA,
    /** El cliente abona $X de saldo y se descuenta por cada salida. */
    ABONO_PREPAGO
}
