// TarifaCalculatorService.java — cálculo del monto (Figura 15)
public double calcular(Ticket ticket, LocalDateTime salida) {
    if (ticket == null) {
        throw new BusinessException("Ticket nulo en calculo", "ERR_TICKET_NULL");
    }
    Tarifa tarifa = ticket.getTarifa();
    if (tarifa == null) {
        throw new BusinessException("Ticket sin tarifa asociada", "ERR_NO_TARIFA");
    }
    if (ticket.getFechaHoraEntrada() == null || salida == null) {
        throw new BusinessException("Fechas invalidas en calculo", "ERR_FECHAS");
    }

    long minutos = Math.max(0L,
            Duration.between(ticket.getFechaHoraEntrada(), salida).toMinutes());
    String unidad = normalizarUnidad(tarifa.getUnidad());
    double valor = tarifa.getValor();

    switch (unidad) {
        case "POR_HORA":
            return Math.ceil(minutos / 60.0) * valor;
        case "POR_FRACCION":
            int frac = tarifa.getMinutosFraccion() != null
                    && tarifa.getMinutosFraccion() > 0
                    ? tarifa.getMinutosFraccion() : 60;
            return Math.ceil((double) minutos / frac) * valor;
        case "POR_DIA":
            return Math.ceil(minutos / 1440.0) * valor;
        case "PLANA":
            return valor;
        default:
            throw new BusinessException(
                    "Unidad de tarifa no soportada: " + tarifa.getUnidad(),
                    "ERR_TARIFA_UNIDAD_DESCONOCIDA");
    }
}
