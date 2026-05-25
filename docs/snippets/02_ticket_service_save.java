// TicketService.java — creación de ticket con bloqueo pesimista (Figura 14)
@Transactional
public TicketDTO save(TicketDTO dto) {
    // RBAC: solo el operador puede registrar entradas
    if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
        throw new AccessDeniedException("Solo el operador puede registrar entradas");
    }

    Parqueadero parqueadero = findParqueadero(dto.getParqueaderoId());
    if (parqueadero.getEmpresa() != null) {
        currentUser.requireEmpresa(parqueadero.getEmpresa().getId());
    }

    // Lock pesimista del punto: serializa intentos concurrentes
    PuntoParqueo punto = puntoParqueoRepository
            .findByIdForUpdate(dto.getPuntoParqueoId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "PuntoParqueo", dto.getPuntoParqueoId()));

    // Validar que el punto NO tenga ticket EN_CURSO
    if (ticketRepository.existsByPuntoParqueoIdAndEstado(punto.getId(), "EN_CURSO")) {
        throw new BusinessException(
                "El punto de parqueo ya esta ocupado", "ERR_POINT_OCCUPIED");
    }

    Ticket entity = new Ticket();
    entity.setParqueadero(parqueadero);
    entity.setPuntoParqueo(punto);
    entity.setVehiculo(findVehiculo(dto.getVehiculoId()));
    entity.setTarifa(findTarifa(dto.getTarifaId()));
    entity.setFechaHoraEntrada(LocalDateTime.now()); // server time, ignora DTO
    entity.setEstado("EN_CURSO");

    Ticket saved = ticketRepository.save(entity);
    eventPublisher.publishEvent(new TicketCreadoEvent(
            this, saved.getId(), parqueadero.getId(), punto.getId()));
    return toDTO(saved);
}
