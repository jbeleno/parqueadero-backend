package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.SubSeccionDTO;
import com.usco.parqueaderos_api.parking.entity.Seccion;
import com.usco.parqueaderos_api.parking.entity.SubSeccion;
import com.usco.parqueaderos_api.parking.repository.SeccionRepository;
import com.usco.parqueaderos_api.parking.repository.SubSeccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubSeccionService {

    private final SubSeccionRepository subSeccionRepository;
    private final SeccionRepository seccionRepository;
    private final EstadoRepository estadoRepository;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<SubSeccionDTO> findAll() {
        List<SubSeccion> base;
        if (currentUser.isSuperAdmin()) {
            base = subSeccionRepository.findAll();
        } else if (currentUser.isAdminParqueadero() || currentUser.isOperarioCaja()) {
            List<Long> parqIds = currentUser.getParqueaderoIds();
            if (parqIds.isEmpty()) return Collections.emptyList();
            base = subSeccionRepository.findAll().stream()
                    .filter(ss -> ss.getSeccion() != null
                            && ss.getSeccion().getParqueadero() != null
                            && parqIds.contains(ss.getSeccion().getParqueadero().getId()))
                    .toList();
        } else {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = subSeccionRepository.findBySeccionParqueaderoEmpresaId(empresaId);
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private Long parqueaderoIdDe(SubSeccion ss) {
        return ss.getSeccion() != null && ss.getSeccion().getParqueadero() != null
                ? ss.getSeccion().getParqueadero().getId() : null;
    }

    @Transactional(readOnly = true)
    public SubSeccionDTO findById(Long id) {
        SubSeccion s = subSeccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id));
        if (s.getSeccion() != null && s.getSeccion().getParqueadero() != null
                && s.getSeccion().getParqueadero().getEmpresa() != null) {
            currentUser.requireEmpresa(s.getSeccion().getParqueadero().getEmpresa().getId());
        }
        return toDTO(s);
    }

    @Transactional
    public SubSeccionDTO save(SubSeccionDTO dto) {
        if (dto.getSeccionId() != null) {
            Seccion sec = findSeccion(dto.getSeccionId());
            if (sec.getParqueadero() != null) {
                if (sec.getParqueadero().getEmpresa() != null) {
                    currentUser.requireEmpresa(sec.getParqueadero().getEmpresa().getId());
                }
                if (currentUser.isAdminParqueadero()) {
                    currentUser.requireParqueadero(sec.getParqueadero().getId());
                }
            }
        }
        return toDTO(subSeccionRepository.save(toEntity(dto)));
    }

    @Transactional
    public SubSeccionDTO update(Long id, SubSeccionDTO dto) {
        SubSeccion existing = subSeccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id));
        Long parqId = parqueaderoIdDe(existing);
        if (parqId != null) {
            if (existing.getSeccion().getParqueadero().getEmpresa() != null) {
                currentUser.requireEmpresa(existing.getSeccion().getParqueadero().getEmpresa().getId());
            }
            if (currentUser.isAdminParqueadero()) {
                currentUser.requireParqueadero(parqId);
            }
        }
        existing.setNombre(dto.getNombre());
        existing.setAcronimo(dto.getAcronimo());
        existing.setDescripcion(dto.getDescripcion());
        if (dto.getSeccionId() != null) existing.setSeccion(findSeccion(dto.getSeccionId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        return toDTO(subSeccionRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        SubSeccion existing = subSeccionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SubSeccion", id));
        Long parqId = parqueaderoIdDe(existing);
        if (parqId != null) {
            if (existing.getSeccion().getParqueadero().getEmpresa() != null) {
                currentUser.requireEmpresa(existing.getSeccion().getParqueadero().getEmpresa().getId());
            }
            if (currentUser.isAdminParqueadero()) {
                currentUser.requireParqueadero(parqId);
            }
        }
        Estado archivado = estadoRepository.findByNombreIgnoreCase("ARCHIVADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        existing.setEstado(archivado);
        subSeccionRepository.save(existing);
    }

    private SubSeccionDTO toDTO(SubSeccion e) {
        SubSeccionDTO dto = new SubSeccionDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setAcronimo(e.getAcronimo());
        dto.setDescripcion(e.getDescripcion());
        if (e.getSeccion() != null) { dto.setSeccionId(e.getSeccion().getId()); dto.setSeccionNombre(e.getSeccion().getNombre()); }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        return dto;
    }

    private SubSeccion toEntity(SubSeccionDTO dto) {
        SubSeccion e = new SubSeccion();
        e.setNombre(dto.getNombre());
        e.setAcronimo(dto.getAcronimo());
        e.setDescripcion(dto.getDescripcion());
        if (dto.getSeccionId() != null) e.setSeccion(findSeccion(dto.getSeccionId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        return e;
    }

    private Seccion findSeccion(Long id) { return seccionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Seccion", id)); }
    private Estado findEstado(Long id) { return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id)); }
}
