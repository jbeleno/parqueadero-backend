// TicketAutoService.java — salida física confirmada dentro de 5 min (Figura 17)
private AutoActionResult procesarSalida(Parqueadero parqueadero, String placa) {
    Vehiculo vehiculo = vehiculoRepo.findByPlaca(placa).orElse(null);
    if (vehiculo == null) {
        return new AutoActionResult(Accion.SALIDA_SIN_TICKET, null, null,
                false, null, null,
                "Vehiculo no registrado — no hay ticket de entrada");
    }

    // Caso 1: hay ticket EN_CURSO → ciclo normal, cierra y cobra
    Optional<Ticket> ticketAbierto = ticketRepo
            .findFirstByVehiculoIdAndParqueaderoIdAndEstadoOrderByFechaHoraEntradaDesc(
                    vehiculo.getId(), parqueadero.getId(), ESTADO_EN_CURSO);

    if (ticketAbierto.isPresent()) {
        Ticket t = ticketAbierto.get();
        LocalDateTime salida = LocalDateTime.now();
        double monto = tarifaCalculator.calcular(t, salida);
        t.setFechaHoraSalida(salida);
        t.setFechaHoraSalidaFisica(salida);
        t.setMontoCalculado(monto);
        t.setEstado("CERRADO");
        ticketRepo.save(t);
        publisher.publishEvent(new TicketCerradoEvent(
                this, t.getId(), parqueadero.getId(), t.getPuntoParqueo().getId()));
        return new AutoActionResult(Accion.SALIDA_REGISTRADA, t.getId(),
                vehiculo.getId(), false, t.getPuntoParqueo().getId(), monto,
                "Salida registrada automaticamente");
    }

    // Caso 2: ticket CERRADO recientemente → salida física esperada
    Optional<Ticket> ultimo = ticketRepo
            .findFirstByVehiculoIdAndParqueaderoIdOrderByFechaHoraEntradaDesc(
                    vehiculo.getId(), parqueadero.getId());
    if (ultimo.isPresent() && "CERRADO".equals(ultimo.get().getEstado())
            && ultimo.get().getFechaHoraSalida() != null) {
        Ticket t = ultimo.get();
        long segundosDesdeCierre = Duration.between(
                t.getFechaHoraSalida(), LocalDateTime.now()).getSeconds();

        if (segundosDesdeCierre <= SALIDA_FISICA_WINDOW_SEC) {
            if (t.getFechaHoraSalidaFisica() == null) {
                t.setFechaHoraSalidaFisica(LocalDateTime.now());
                ticketRepo.save(t);
            }
            return new AutoActionResult(Accion.SALIDA_CONFIRMADA_FISICA,
                    t.getId(), vehiculo.getId(), false,
                    t.getPuntoParqueo().getId(), t.getMontoCalculado(),
                    "Salida fisica confirmada — ticket ya estaba cerrado");
        }
    }

    // Caso 3: sin ticket reciente → alerta de salida fantasma
    return new AutoActionResult(Accion.SALIDA_SIN_TICKET, null, vehiculo.getId(),
            false, null, null,
            "No hay ticket reciente del vehiculo en este parqueadero");
}
