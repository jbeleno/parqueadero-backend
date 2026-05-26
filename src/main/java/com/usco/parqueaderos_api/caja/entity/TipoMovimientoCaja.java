package com.usco.parqueaderos_api.caja.entity;

public enum TipoMovimientoCaja {
    APERTURA,       // fondo inicial al abrir la caja
    INGRESO_PAGO,   // pago EFECTIVO de un ticket
    RETIRO,         // operador/admin retira efectivo (deposito al banco)
    DEPOSITO,       // entra efectivo para cambio
    AJUSTE,         // ajuste manual con motivo (faltante/sobrante post-arqueo)
    CIERRE          // snapshot al cerrar
}
