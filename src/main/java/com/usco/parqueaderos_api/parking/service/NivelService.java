package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.NivelDTO;
import com.usco.parqueaderos_api.parking.entity.Nivel;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.NivelRepository;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NivelService {

    private final NivelRepository nivelRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final EstadoRepository estadoRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<NivelDTO> findAll() {
        List<Nivel> base;
        if (currentUser.isSuperAdmin()) {
            base = nivelRepository.findAll();
        } else if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            List<Long> parqIds = currentUser.getParqueaderoIds();
            if (parqIds.isEmpty()) return Collections.emptyList();
            base = nivelRepository.findAll().stream()
                    .filter(n -> n.getParqueadero() != null
                            && parqIds.contains(n.getParqueadero().getId()))
                    .toList();
        } else {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = nivelRepository.findByParqueaderoEmpresaId(empresaId);
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NivelDTO> findByParqueadero(Long parqueaderoId) {
        if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            currentUser.requireParqueadero(parqueaderoId);
        }
        return nivelRepository.findByParqueaderoId(parqueaderoId).stream()
                .filter(n -> currentUser.isSuperAdmin()
                        || (n.getParqueadero().getEmpresa() != null
                            && currentUser.getCurrentEmpresaId().filter(eid -> eid.equals(n.getParqueadero().getEmpresa().getId())).isPresent()))
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NivelDTO findById(Long id) {
        Nivel n = nivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel", id));
        if (n.getParqueadero() != null && n.getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(n.getParqueadero().getEmpresa().getId());
        }
        return toDTO(n);
    }

    @Transactional
    public NivelDTO save(NivelDTO dto) {
        if (dto.getParqueaderoId() != null) {
            Parqueadero p = findParqueadero(dto.getParqueaderoId());
            if (p.getEmpresa() != null) currentUser.requireEmpresa(p.getEmpresa().getId());
            if (currentUser.isAdminParqueadero()) currentUser.requireParqueadero(p.getId());
        }
        return toDTO(nivelRepository.save(toEntity(dto)));
    }

    @Transactional
    public NivelDTO update(Long id, NivelDTO dto) {
        Nivel existing = nivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel", id));
        if (existing.getParqueadero() != null) {
            if (existing.getParqueadero().getEmpresa() != null) {
                currentUser.requireEmpresa(existing.getParqueadero().getEmpresa().getId());
            }
            if (currentUser.isAdminParqueadero()) {
                currentUser.requireParqueadero(existing.getParqueadero().getId());
            }
        }
        existing.setNombre(dto.getNombre());
        if (dto.getParqueaderoId() != null) existing.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        return toDTO(nivelRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        Nivel existing = nivelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel", id));
        if (existing.getParqueadero() != null) {
            if (existing.getParqueadero().getEmpresa() != null) {
                currentUser.requireEmpresa(existing.getParqueadero().getEmpresa().getId());
            }
            if (currentUser.isAdminParqueadero()) {
                currentUser.requireParqueadero(existing.getParqueadero().getId());
            }
        }
        Estado archivado = estadoRepository.findByNombreIgnoreCase("ARCHIVADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        existing.setEstado(archivado);
        nivelRepository.save(existing);
    }

    private NivelDTO toDTO(Nivel e) {
        NivelDTO dto = new NivelDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        if (e.getParqueadero() != null) { dto.setParqueaderoId(e.getParqueadero().getId()); dto.setParqueaderoNombre(e.getParqueadero().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        return dto;
    }

    private Nivel toEntity(NivelDTO dto) {
        Nivel e = new Nivel();
        e.setNombre(dto.getNombre());
        if (dto.getParqueaderoId() != null) e.setParqueadero(findParqueadero(dto.getParqueaderoId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private Parqueadero findParqueadero(Long id) {
        return parqueaderoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
    }
    private Estado findEstado(Long id) {
        return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id));
    }
}
