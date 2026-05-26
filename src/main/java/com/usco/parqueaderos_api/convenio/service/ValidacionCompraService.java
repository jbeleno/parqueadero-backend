package com.usco.parqueaderos_api.convenio.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.convenio.dto.ValidacionCompraDTO;
import com.usco.parqueaderos_api.convenio.entity.Convenio;
import com.usco.parqueaderos_api.convenio.entity.ValidacionCompra;
import com.usco.parqueaderos_api.convenio.repository.ConvenioRepository;
import com.usco.parqueaderos_api.convenio.repository.ValidacionCompraRepository;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra el comprobante de compra del comercio sobre un ticket EN_CURSO.
 * El descuento efectivo se materializa cuando se cierra el ticket
 * (TicketService consulta la ultima ValidacionCompra del ticket).
 */
@Service
@RequiredArgsConstructor
public class ValidacionCompraService {

    private final ValidacionCompraRepository repository;
    private final ConvenioRepository convenioRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUser;

    @Transactional
    public ValidacionCompraDTO registrar(ValidacionCompraDTO dto) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo operador puede validar compras");
        }
        if (dto.getTicketId() == null || dto.getConvenioId() == null) {
            throw new BusinessException("ticketId y convenioId obligatorios", "ERR_MISSING_FIELDS");
        }
        if (dto.getMontoCompra() == null || dto.getMontoCompra() <= 0) {
            throw new BusinessException("montoCompra > 0", "ERR_INVALID_VALUE");
        }
        Ticket t = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", dto.getTicketId()));
        Convenio c = convenioRepository.findById(dto.getConvenioId())
                .orElseThrow(() -> new ResourceNotFoundException("Convenio", dto.getConvenioId()));

        // Multi-tenant: convenio y ticket deben ser del mismo parqueadero
        if (t.getParqueadero() == null || c.getParqueadero() == null
                || !t.getParqueadero().getId().equals(c.getParqueadero().getId())) {
            throw new BusinessException(
                    "El convenio no aplica a este parqueadero",
                    "ERR_CONVENIO_PARQUEADERO_MISMATCH");
        }
        if (!"EN_CURSO".equals(t.getEstado())) {
            throw new BusinessException(
                    "Solo se puede validar compra en tickets EN_CURSO",
                    "ERR_TICKET_ESTADO");
        }
        if (!currentUser.isSuperAdmin() && t.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(t.getParqueadero().getEmpresa().getId());
        }

        ValidacionCompra v = new ValidacionCompra();
        v.setTicket(t);
        v.setConvenio(c);
        v.setMontoCompra(dto.getMontoCompra());
        v.setFolioExterno(dto.getFolioExterno());
        // fechaAplicacion ya tiene default = LocalDateTime.now()
        return toDTO(repository.save(v));
    }

    private ValidacionCompraDTO toDTO(ValidacionCompra v) {
        return new ValidacionCompraDTO(
                v.getId(),
                v.getTicket() != null ? v.getTicket().getId() : null,
                v.getConvenio() != null ? v.getConvenio().getId() : null,
                v.getConvenio() != null ? v.getConvenio().getNombreComercio() : null,
                v.getMontoCompra(),
                v.getFolioExterno(),
                v.getFechaAplicacion(),
                v.getDescuentoAplicado()
        );
    }
}
