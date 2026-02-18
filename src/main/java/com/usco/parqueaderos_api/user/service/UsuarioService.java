package com.usco.parqueaderos_api.user.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.DuplicateResourceException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.repository.EmpresaRepository;
import com.usco.parqueaderos_api.user.dto.UsuarioDTO;
import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.repository.PersonaRepository;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final EstadoRepository estadoRepository;
    private final EmpresaRepository empresaRepository;

    @Transactional(readOnly = true)
    public List<UsuarioDTO> findAll() {
        return usuarioRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioDTO findById(Long id) {
        return usuarioRepository.findById(id).map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    @Transactional
    public UsuarioDTO save(UsuarioDTO dto) {
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo: " + dto.getCorreo());
        }
        Usuario entity = toEntity(dto);
        entity.setFechaCreacion(LocalDateTime.now());
        return toDTO(usuarioRepository.save(entity));
    }

    @Transactional
    public UsuarioDTO update(Long id, UsuarioDTO dto) {
        Usuario existing = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        existing.setCorreo(dto.getCorreo());
        if (dto.getPersonaId() != null) existing.setPersona(findPersona(dto.getPersonaId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        if (dto.getEmpresaId() != null) existing.setEmpresa(findEmpresa(dto.getEmpresaId()));
        return toDTO(usuarioRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        Usuario existing = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        Estado archivado = estadoRepository.findByNombreIgnoreCase("ARCHIVADO")
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        existing.setEstado(archivado);
        usuarioRepository.save(existing);
    }

    private UsuarioDTO toDTO(Usuario e) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(e.getId());
        dto.setCorreo(e.getCorreo());
        dto.setFechaCreacion(e.getFechaCreacion());
        if (e.getPersona() != null) {
            dto.setPersonaId(e.getPersona().getId());
            dto.setPersonaNombre(e.getPersona().getNombre() + " " + e.getPersona().getApellido());
            dto.setPersonaDocumento(e.getPersona().getNumeroDocumento());
        }
        if (e.getEstado() != null) { dto.setEstadoId(e.getEstado().getId()); dto.setEstadoNombre(e.getEstado().getNombre()); }
        if (e.getEmpresa() != null) { dto.setEmpresaId(e.getEmpresa().getId()); dto.setEmpresaNombre(e.getEmpresa().getNombre()); }
        return dto;
    }

    private Usuario toEntity(UsuarioDTO dto) {
        Usuario e = new Usuario();
        e.setCorreo(dto.getCorreo());
        e.setPasswordHash(""); // password set separately by auth flow
        if (dto.getPersonaId() != null) e.setPersona(findPersona(dto.getPersonaId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        if (dto.getEmpresaId() != null) e.setEmpresa(findEmpresa(dto.getEmpresaId()));
        return e;
    }

    private Persona findPersona(Long id) { return personaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Persona", id)); }
    private Estado findEstado(Long id) { return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id)); }
    private Empresa findEmpresa(Long id) { return empresaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Empresa", id)); }
}
