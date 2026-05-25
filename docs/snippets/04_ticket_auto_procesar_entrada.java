// TicketAutoService.java — flujo automático de entrada (Figura 16)
private AutoActionResult procesarEntrada(Parqueadero parqueadero, String placa) {
    boolean vehiculoCreado = false;
    Vehiculo vehiculo = vehiculoRepo.findByPlaca(placa).orElse(null);
    if (vehiculo == null) {
        try {
            vehiculo = crearVehiculoInvitado(placa);
            vehiculoCreado = true;
            log.info("Creado vehiculo invitado para placa {}", placa);
        } catch (DataIntegrityViolationException ex) {
            // Race condition: otro thread con misma placa nos gano. Reusar.
            vehiculo = vehiculoRepo.findByPlaca(placa).orElseThrow(() -> ex);
        }
    }

    // ¿Ya tiene ticket EN_CURSO en este parqueadero?
    if (ticketRepo.existsByVehiculoIdAndParqueaderoIdAndEstado(
            vehiculo.getId(), parqueadero.getId(), ESTADO_EN_CURSO)) {
        return new AutoActionResult(Accion.ENTRADA_DUPLICADA, null,
                vehiculo.getId(), vehiculoCreado, null, null,
                "Vehiculo ya tiene ticket abierto en este parqueadero");
    }

    // Buscar punto libre + lock pesimista (serializa entradas concurrentes)
    PuntoParqueo puntoLibre = buscarYLockearPuntoLibre(parqueadero.getId());
    if (puntoLibre == null) {
        return new AutoActionResult(Accion.ERROR, null, vehiculo.getId(),
                vehiculoCreado, null, null, "No hay puntos de parqueo libres");
    }

    Tarifa tarifa = buscarTarifa(parqueadero.getId(), vehiculo.getTipoVehiculo());
    Ticket t = new Ticket();
    t.setParqueadero(parqueadero);
    t.setPuntoParqueo(puntoLibre);
    t.setVehiculo(vehiculo);
    t.setTarifa(tarifa);
    t.setFechaHoraEntrada(LocalDateTime.now());
    t.setEstado(ESTADO_EN_CURSO);
    Ticket saved = ticketRepo.save(t);

    publisher.publishEvent(new TicketCreadoEvent(
            this, saved.getId(), parqueadero.getId(), puntoLibre.getId()));

    return new AutoActionResult(Accion.ENTRADA_REGISTRADA, saved.getId(),
            vehiculo.getId(), vehiculoCreado, puntoLibre.getId(), null,
            "Entrada registrada automaticamente");
}
