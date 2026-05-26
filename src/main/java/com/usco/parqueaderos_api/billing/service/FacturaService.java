package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.FacturaDTO;
import com.usco.parqueaderos_api.billing.entity.Factura;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService;
import com.usco.parqueaderos_api.tariff.service.TarifaCalculatorService.BreakdownIva;
import com.usco.parqueaderos_api.ticket.entity.Ticket;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final TicketRepository ticketRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final TarifaCalculatorService tarifaCalculator;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<FacturaDTO> findAll() {
        List<Factura> base;
        if (currentUser.isSuperAdmin()) {
            base = facturaRepository.findAll();
        } else if (currentUser.isAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = facturaRepository.findByParqueaderoEmpresaId(empresaId);
        } else {
            base = facturaRepository.findByVehiculoPersonaId(currentUser.getCurrentPersonaId());
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FacturaDTO findById(Long id) {
        Factura f = facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", id));
        if (currentUser.isSuperAdmin()) return toDTO(f);
        if (currentUser.isAdmin()) {
            if (f.getParqueadero() != null && f.getParqueadero().getEmpresa() != null) {
                currentUser.requireEmpresa(f.getParqueadero().getEmpresa().getId());
            }
        } else {
            Long personaId = f.getVehiculo() != null && f.getVehiculo().getPersona() != null
                    ? f.getVehiculo().getPersona().getId() : null;
            currentUser.requireOwnerOrAnyAdmin(personaId);
        }
        return toDTO(f);
    }

    /**
     * Crea una factura. Blindado:
     * - RBAC: solo ADMIN/SUPER_ADMIN
     * - Multi-tenant: parqueadero debe ser de la empresa del operador
     * - Invariantes: ticket debe estar CERRADO con monto. valorTotal del
     *   cliente se IGNORA, se toma del ticket.montoCalculado para evitar
     *   manipulacion.
     */
    @Transactional
    public FacturaDTO save(FacturaDTO dto) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo el operador puede generar facturas");
        }
        if (dto.getTicketId() == null) {
            throw new BusinessException("ticketId es obligatorio", "ERR_MISSING_FIELDS");
        }
        Ticket ticket = findTicket(dto.getTicketId());

        // Multi-tenant
        if (ticket.getParqueadero() != null && ticket.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(ticket.getParqueadero().getEmpresa().getId());
        }

        // El ticket debe estar CERRADO con monto calculado
        if (!"CERRADO".equals(ticket.getEstado())) {
            throw new BusinessException(
                    "El ticket debe estar CERRADO para facturar. Estado actual: " + ticket.getEstado(),
                    "ERR_TICKET_NOT_CLOSED");
        }
        if (ticket.getMontoCalculado() == null || ticket.getMontoCalculado() <= 0) {
            throw new BusinessException(
                    "El ticket no tiene monto calculado",
                    "ERR_TICKET_NO_AMOUNT");
        }

        // Evitar duplicar facturas vigentes (no-anuladas) para el mismo ticket.
        // Consistente con UNIQUE INDEX parcial uniq_factura_ticket_no_anulada.
        boolean yaTieneVigente = facturaRepository.findByTicketId(ticket.getId()).stream()
                .anyMatch(f -> !"ANULADA".equals(f.getEstado()));
        if (yaTieneVigente) {
            throw new BusinessException(
                    "El ticket ya tiene una factura vigente asociada",
                    "ERR_INVOICE_DUPLICATE");
        }

        // Forzar valorTotal = monto del ticket (no leer del DTO)
        Factura entity = new Factura();
        entity.setTicket(ticket);
        entity.setParqueadero(ticket.getParqueadero());
        entity.setVehiculo(ticket.getVehiculo());
        entity.setValorTotal(ticket.getMontoCalculado());
        entity.setFechaHora(LocalDateTime.now());
        entity.setEstado("PENDIENTE");
        entity.setOrigen("MANUAL");
        // Desagregar IVA si la tarifa lo aplica
        if (ticket.getTarifa() != null) {
            BreakdownIva b = tarifaCalculator.desagregarIva(ticket.getTarifa(), ticket.getMontoCalculado());
            if (b.iva() > 0.0) {
                entity.setBaseImponible(b.base());
                entity.setIvaMonto(b.iva());
                entity.setIvaPorcentaje(ticket.getTarifa().getIvaPorcentaje());
            }
        }
        return toDTO(facturaRepository.save(entity));
    }

    /**
     * Solo SUPER_ADMIN puede modificar una factura ya emitida (correccion).
     * No se permite cambiar valorTotal — ese es el del ticket. Solo estado
     * para casos de anulacion.
     */
    @Transactional
    public FacturaDTO update(Long id, FacturaDTO dto) {
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede modificar una factura emitida");
        }
        Factura existing = facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", id));
        if (dto.getEstado() != null) {
            existing.setEstado(dto.getEstado());
        }
        return toDTO(facturaRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede eliminar facturas");
        }
        facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", id));
        facturaRepository.deleteById(id);
    }

    private FacturaDTO toDTO(Factura e) {
        FacturaDTO dto = new FacturaDTO();
        dto.setId(e.getId());
        dto.setFechaHora(e.getFechaHora());
        dto.setValorTotal(e.getValorTotal());
        dto.setEstado(e.getEstado());
        if (e.getTicket() != null) dto.setTicketId(e.getTicket().getId());
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getVehiculo() != null) { dto.setVehiculoId(e.getVehiculo().getId()); dto.setVehiculoPlaca(e.getVehiculo().getPlaca()); }
        dto.setBaseImponible(e.getBaseImponible());
        dto.setIvaMonto(e.getIvaMonto());
        dto.setIvaPorcentaje(e.getIvaPorcentaje());
        dto.setOrigen(e.getOrigen());
        return dto;
    }

    private Ticket findTicket(Long id) { return ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", id)); }
}
