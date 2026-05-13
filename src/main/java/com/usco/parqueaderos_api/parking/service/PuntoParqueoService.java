package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoPuntoParqueo;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoPuntoParqueoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.PuntoParqueoDTO;
import com.usco.parqueaderos_api.parking.entity.PuntoParqueo;
import com.usco.parqueaderos_api.parking.entity.SubSeccion;
import com.usco.parqueaderos_api.parking.repository.PuntoParqueoRepository;
import com.usco.parqueaderos_api.parking.repository.SubSeccionRepository;
import com.usco.parqueaderos_api.reservation.repository.ReservaRepository;
import com.usco.parqueaderos_api.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PuntoParqueoService {

    private final PuntoParqueoRepository puntoParqueoRepository;
    private final SubSeccionRepository subSeccionRepository;
    private final TipoPuntoParqueoRepository tipoPuntoParqueoRepository;
    private final EstadoRepository estadoRepository;
    private final TicketRepository ticketRepository;
    private final ReservaRepository reservaRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<PuntoParqueoDTO> findAll() {
        List<PuntoParqueo> base;
        if (currentUser.isSuperAdmin()) {
            base = puntoParqueoRepository.findAll();
        } else {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = puntoParqueoRepository.findBySubSeccionSeccionParqueaderoEmpresaId(empresaId);
        }
        return mapAndCalcularEstadoBatch(base);
    }

    @Transactional(readOnly = true)
    public PuntoParqueoDTO findById(Long id) {
        PuntoParqueo p = puntoParqueoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", id));
        if (p.getSubSeccion() != null && p.getSubSeccion().getSeccion() != null
                && p.getSubSeccion().getSeccion().getParqueadero() != null
                && p.getSubSeccion().getSeccion().getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(p.getSubSeccion().getSeccion().getParqueadero().getEmpresa().getId());
        }
        return toDTOConEstado(p);
    }

    @Transactional
    public PuntoParqueoDTO save(PuntoParqueoDTO dto) {
        return toDTOConEstado(puntoParqueoRepository.save(toEntity(dto)));
    }

    @Transactional
    public PuntoParqueoDTO update(Long id, PuntoParqueoDTO dto) {
        PuntoParqueo existing = puntoParqueoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", id));
        existing.setNombre(dto.getNombre());
        existing.setAcronimo(dto.getAcronimo());
        existing.setDescripcion(dto.getDescripcion());
        if (dto.getSubSeccionId() != null) existing.setSubSeccion(findSubSeccion(dto.getSubSeccionId()));
        if (dto.getTipoPuntoParqueoId() != null) existing.setTipoPuntoParqueo(findTipo(dto.getTipoPuntoParqueoId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        return toDTOConEstado(puntoParqueoRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        PuntoParqueo existing = puntoParqueoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PuntoParqueo", id));
        Estado archivado = estadoRepository.findByNombreIgnoreCase("ARCHIVADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        existing.setEstado(archivado);
        puntoParqueoRepository.save(existing);
    }

    /**
     * Mapea la lista a DTOs calculando estadoOperativo en batch (2 queries totales,
     * no 2 por punto). Evita N+1.
     */
    private List<PuntoParqueoDTO> mapAndCalcularEstadoBatch(List<PuntoParqueo> entities) {
        if (entities.isEmpty()) return Collections.emptyList();
        List<Long> ids = entities.stream().map(PuntoParqueo::getId).collect(Collectors.toList());
        Set<Long> ocupados = puntoParqueoRepository.idsOcupadosEntre(ids);
        Set<Long> reservados = puntoParqueoRepository.idsReservadosEntre(ids, LocalDateTime.now());
        return entities.stream().map(e -> {
            PuntoParqueoDTO dto = toDTO(e);
            String op;
            if (ocupados.contains(e.getId())) op = "OCUPADO";
            else if (reservados.contains(e.getId())) op = "RESERVADO";
            else op = "DISPONIBLE";
            dto.setEstadoOperativo(op);
            dto.setEstado(toLowerEstado(op));
            return dto;
        }).collect(Collectors.toList());
    }

    /** Para un solo punto: 2 queries simples. */
    private PuntoParqueoDTO toDTOConEstado(PuntoParqueo e) {
        PuntoParqueoDTO dto = toDTO(e);
        String op = calcularEstadoOperativo(e.getId());
        dto.setEstadoOperativo(op);
        dto.setEstado(toLowerEstado(op));
        return dto;
    }

    private String calcularEstadoOperativo(Long puntoId) {
        if (ticketRepository.existsByPuntoParqueoIdAndEstado(puntoId, "EN_CURSO")) {
            return "OCUPADO";
        }
        if (reservaRepository.existsReservaActivaParaPunto(puntoId, LocalDateTime.now())) {
            return "RESERVADO";
        }
        return "DISPONIBLE";
    }

    /** OCUPADO->occupied, RESERVADO->reserved, DISPONIBLE->free (formato frontend). */
    private String toLowerEstado(String op) {
        if (op == null) return "free";
        switch (op) {
            case "OCUPADO":   return "occupied";
            case "RESERVADO": return "reserved";
            default:          return "free";
        }
    }

    /**
     * Lista los puntos no archivados de un parqueadero con su estado operativo
     * calculado. Accesible para cualquier usuario autenticado (sin filtrado
     * multi-tenant porque es lectura del layout publico del parqueadero).
     */
    @Transactional(readOnly = true)
    public List<PuntoParqueoDTO> findByParqueadero(Long parqueaderoId) {
        List<PuntoParqueo> base = puntoParqueoRepository.findActiveByParqueaderoId(parqueaderoId);
        return mapAndCalcularEstadoBatch(base);
    }

    private PuntoParqueoDTO toDTO(PuntoParqueo e) {
        PuntoParqueoDTO dto = new PuntoParqueoDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setAcronimo(e.getAcronimo());
        dto.setDescripcion(e.getDescripcion());
        if (e.getSubSeccion() != null) { dto.setSubSeccionId(e.getSubSeccion().getId()); dto.setSubSeccionNombre(e.getSubSeccion().getNombre()); }
        if (e.getTipoPuntoParqueo() != null) { dto.setTipoPuntoParqueoId(e.getTipoPuntoParqueo().getId()); dto.setTipoPuntoParqueoNombre(e.getTipoPuntoParqueo().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        return dto;
    }

    private PuntoParqueo toEntity(PuntoParqueoDTO dto) {
        PuntoParqueo e = new PuntoParqueo();
        e.setNombre(dto.getNombre());
        e.setAcronimo(dto.getAcronimo());
        e.setDescripcion(dto.getDescripcion());
        if (dto.getSubSeccionId() != null) e.setSubSeccion(findSubSeccion(dto.getSubSeccionId()));
        if (dto.getTipoPuntoParqueoId() != null) e.setTipoPuntoParqueo(findTipo(dto.getTipoPuntoParqueoId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private SubSeccion findSubSeccion(Long id) { return subSeccionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id)); }
    private TipoPuntoParqueo findTipo(Long id) { return tipoPuntoParqueoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("TipoPuntoParqueo", id)); }
    private Estado findEstado(Long id) { return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id)); }
}
