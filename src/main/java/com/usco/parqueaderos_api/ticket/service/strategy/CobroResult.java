package com.usco.parqueaderos_api.ticket.service.strategy;

/**
 * Resultado de aplicar un CobroStrategy a un ticket que se esta cerrando.
 *
 * @param montoCobrado     monto efectivamente cobrado (puede ser 0 si cubierto por suscripcion).
 * @param suscripcionId    id de la Suscripcion que cubrio el ticket (null si fue tarifa normal).
 * @param facturaPendiente true si se debe crear una Factura PENDIENTE por monto_cobrado.
 *                         false si ya fue pagado (suscripcion o ABONO_PREPAGO).
 */
public record CobroResult(
        double montoCobrado,
        Long suscripcionId,
        boolean facturaPendiente,
        String mensaje
) {
}
