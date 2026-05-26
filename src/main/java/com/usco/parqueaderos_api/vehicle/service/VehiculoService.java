package com.usco.parqueaderos_api.vehicle.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.repository.FacturaRepository;
import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.catalog.repository.TipoVehiculoRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.DuplicateResourceException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.reservation.repository.ReservaRepository;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.user.repository.PersonaRepository;
import com.usco.parqueaderos_api.vehicle.dto.VehiculoDTO;
import com.usco.parqueaderos_api.vehicle.entity.Vehiculo;
import com.usco.parqueaderos_api.vehicle.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final PersonaRepository personaRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final TicketRepository ticketRepository;
    private final ReservaRepository reservaRepository;
    private final FacturaRepository facturaRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<VehiculoDTO> findAll() {
        return findAll(false, false);
    }

    /** Compat: deja la firma vieja apuntando a la nueva con flags por defecto. */
    @Transactional(readOnly = true)
    public List<VehiculoDTO> findAll(boolean soloMiEmpresa) {
        return findAll(soloMiEmpresa, false);
    }

    /**
     * Lista vehiculos.
     * - SUPER_ADMIN: siempre ve todos (ignora soloMiEmpresa)
     * - ADMIN:
     *     soloMiEmpresa=false (default): ve todos (necesario para registrar
     *         entradas de visitantes cuyo vehiculo aun no esta en su empresa)
     *     soloMiEmpresa=true: filtra por historial - solo vehiculos que ya
     *         hayan tenido ticket o reserva en algun parqueadero de su empresa
     * - USER: ignora el flag, solo ve sus propios vehiculos
     */
    @Transactional(readOnly = true)
    public List<VehiculoDTO> findAll(boolean soloMiEmpresa, boolean incluirArchivados) {
        List<Vehiculo> base;
        if (currentUser.isSuperAdmin()) {
            base = vehiculoRepository.findAll();
        } else if (currentUser.isAdmin()) {
            if (soloMiEmpresa) {
                Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
                if (empresaId == null) return java.util.Collections.emptyList();
                base = vehiculoRepository.findByActividadEnEmpresa(empresaId);
            } else {
                base = vehiculoRepository.findAll();
            }
        } else {
            // USER: solo sus propios vehiculos
            Long personaId = currentUser.getCurrentPersonaId();
            base = vehiculoRepository.findByPersonaId(personaId);
        }
        if (!incluirArchivados) {
            base = base.stream()
                    .filter(v -> v.getActivo() == null || v.getActivo())
                    .toList();
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Soft-delete: marca activo=false. No borra historial. */
    @Transactional
    public VehiculoDTO archivar(Long id) {
        Vehiculo v = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id));
        // No archivar si tiene ticket EN_CURSO
        if (ticketRepository.existsByVehiculoIdAndParqueaderoIdAndEstado(id, 0L, "EN_CURSO")
                || tieneTicketEnCurso(id)) {
            throw new BusinessException(
                    "No se puede archivar: el vehiculo tiene un ticket EN_CURSO",
                    "ERR_VEHICULO_EN_CURSO");
        }
        v.setActivo(false);
        v.setArchivadoEn(java.time.LocalDateTime.now());
        return toDTO(vehiculoRepository.save(v));
    }

    private boolean tieneTicketEnCurso(Long vehiculoId) {
        // existsByVehiculoIdAndParqueaderoIdAndEstado requiere parqueaderoId, evitamos
        // buscando si countByVehiculoId del estado EN_CURSO en cualquier parqueadero.
        return ticketRepository.findByVehiculoId(vehiculoId).stream()
                .anyMatch(t -> "EN_CURSO".equals(t.getEstado()));
    }

    @Transactional
    public VehiculoDTO desarchivar(Long id) {
        Vehiculo v = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id));
        v.setActivo(true);
        v.setArchivadoEn(null);
        return toDTO(vehiculoRepository.save(v));
    }

    @Transactional(readOnly = true)
    public VehiculoDTO findById(Long id) {
        Vehiculo v = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id));
        Long personaId = v.getPersona() != null ? v.getPersona().getId() : null;
        currentUser.requireOwnerOrAnyAdmin(personaId);
        return toDTO(v);
    }

    @Transactional
    public VehiculoDTO save(VehiculoDTO dto) {
        vehiculoRepository.findByPlaca(dto.getPlaca()).ifPresent(v -> {
            throw new DuplicateResourceException("Ya existe un vehículo con la placa: " + dto.getPlaca());
        });
        return toDTO(vehiculoRepository.save(toEntity(dto)));
    }

    @Transactional
    public VehiculoDTO update(Long id, VehiculoDTO dto) {
        Vehiculo existing = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id));
        existing.setPlaca(dto.getPlaca());
        existing.setColor(dto.getColor());
        if (dto.getPersonaId() != null) existing.setPersona(findPersona(dto.getPersonaId()));
        if (dto.getTipoVehiculoId() != null) existing.setTipoVehiculo(findTipo(dto.getTipoVehiculoId()));
        return toDTO(vehiculoRepository.save(existing));
    }

    /**
     * Borrado seguro: solo permite eliminar si el vehiculo no tiene historial
     * (tickets, reservas ni facturas). Si lo tiene, devuelve un error legible
     * en vez de dejar que la FK rompa con un 500 opaco. El front puede entonces
     * sugerir "archivar"/marcar inactivo en lugar de borrar.
     */
    @Transactional
    public void delete(Long id) {
        vehiculoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id));
        long tickets = ticketRepository.countByVehiculoId(id);
        long reservas = reservaRepository.countByVehiculoId(id);
        long facturas = facturaRepository.countByVehiculoId(id);
        if (tickets > 0 || reservas > 0 || facturas > 0) {
            throw new BusinessException(
                    "No se puede eliminar el vehiculo: tiene historial ("
                            + tickets + " tickets, " + reservas + " reservas, "
                            + facturas + " facturas). Reasigne o archive el dato.",
                    "ERR_VEHICULO_CON_HISTORIAL");
        }
        vehiculoRepository.deleteById(id);
    }

    private VehiculoDTO toDTO(Vehiculo e) {
        VehiculoDTO dto = new VehiculoDTO();
        dto.setId(e.getId());
        dto.setPlaca(e.getPlaca());
        dto.setColor(e.getColor());
        if (e.getPersona() != null) {
            dto.setPersonaId(e.getPersona().getId());
            dto.setPersonaNombre(e.getPersona().getNombre() + " " + e.getPersona().getApellido());
            dto.setPersonaDocumento(e.getPersona().getNumeroDocumento());
        }
        if (e.getTipoVehiculo() != null) { dto.setTipoVehiculoId(e.getTipoVehiculo().getId()); dto.setTipoVehiculoNombre(e.getTipoVehiculo().getNombre()); }
        dto.setActivo(e.getActivo() == null || e.getActivo());
        dto.setEsVisitante(e.getEsVisitante() != null && e.getEsVisitante());
        dto.setUltimaActividad(e.getUltimaActividad());
        dto.setArchivadoEn(e.getArchivadoEn());
        return dto;
    }

    private Vehiculo toEntity(VehiculoDTO dto) {
        Vehiculo e = new Vehiculo();
        e.setPlaca(dto.getPlaca());
        e.setColor(dto.getColor());
        if (dto.getPersonaId() != null) e.setPersona(findPersona(dto.getPersonaId()));
        if (dto.getTipoVehiculoId() != null) e.setTipoVehiculo(findTipo(dto.getTipoVehiculoId()));
        return e;
    }

    private Persona findPersona(Long id) { return personaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Persona", id)); }
    private TipoVehiculo findTipo(Long id) { return tipoVehiculoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", id)); }
}
