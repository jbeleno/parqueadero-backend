package com.usco.parqueaderos_api.user.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final EstadoRepository estadoRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<UsuarioDTO> findAll() {
        List<Usuario> base;
        if (currentUser.isSuperAdmin()) {
            base = usuarioRepository.findAll();
        } else if (currentUser.isAdmin()) {
            Long empresaId = currentUser.getCurrentEmpresaId().orElse(null);
            if (empresaId == null) return Collections.emptyList();
            base = usuarioRepository.findByEmpresaId(empresaId);
        } else {
            // USER: solo se ve a si mismo
            return List.of(toDTO(currentUser.getCurrent()));
        }
        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UsuarioDTO findById(Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        if (currentUser.isSuperAdmin()) return toDTO(u);
        if (currentUser.isAdmin()) {
            if (u.getEmpresa() != null) {
                currentUser.requireEmpresa(u.getEmpresa().getId());
            } else {
                throw new AccessDeniedException("Usuario sin empresa, solo SUPER_ADMIN puede verlo");
            }
        } else {
            // USER: solo se ve a si mismo
            if (!u.getId().equals(currentUser.getCurrentUserId())) {
                throw new AccessDeniedException("No puedes ver otros usuarios");
            }
        }
        return toDTO(u);
    }

    @Transactional
    public UsuarioDTO save(UsuarioDTO dto) {
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo: " + dto.getCorreo());
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException("La password es obligatoria al crear un usuario");
        }
        // Solo SUPER_ADMIN puede asignar empresa distinta a la propia
        if (!currentUser.isSuperAdmin() && dto.getEmpresaId() != null) {
            Long miEmpresa = currentUser.getCurrentEmpresaId().orElse(null);
            if (!dto.getEmpresaId().equals(miEmpresa)) {
                throw new AccessDeniedException("No puedes asignar usuarios a otra empresa");
            }
        }
        Usuario entity = toEntity(dto);
        entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setConfirmado(dto.getConfirmado() != null ? dto.getConfirmado() : true);
        entity.setIntentosFallidos(0);
        return toDTO(usuarioRepository.save(entity));
    }

    @Transactional
    public UsuarioDTO update(Long id, UsuarioDTO dto) {
        Usuario existing = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        // Validar acceso al usuario que se modifica
        if (!currentUser.isSuperAdmin()) {
            if (currentUser.isAdmin()) {
                if (existing.getEmpresa() == null
                        || !existing.getEmpresa().getId().equals(currentUser.getCurrentEmpresaId().orElse(null))) {
                    throw new AccessDeniedException("Usuario no pertenece a tu empresa");
                }
            } else {
                if (!existing.getId().equals(currentUser.getCurrentUserId())) {
                    throw new AccessDeniedException("Solo puedes editar tu propio usuario");
                }
            }
        }
        // R-4: Solo SUPER_ADMIN puede cambiar la empresa de un usuario
        if (dto.getEmpresaId() != null
                && (existing.getEmpresa() == null || !existing.getEmpresa().getId().equals(dto.getEmpresaId()))
                && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede cambiar la empresa de un usuario");
        }
        existing.setCorreo(dto.getCorreo());
        if (dto.getPersonaId() != null) existing.setPersona(findPersona(dto.getPersonaId()));
        if (dto.getEstadoId() != null) existing.setEstado(findEstado(dto.getEstadoId()));
        if (dto.getEmpresaId() != null) existing.setEmpresa(findEmpresa(dto.getEmpresaId()));
        if (dto.getConfirmado() != null) existing.setConfirmado(dto.getConfirmado());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        return toDTO(usuarioRepository.save(existing));
    }

    /** Soft-delete: cambia el estado a ARCHIVADO */
    @Transactional
    public void archivar(Long id) {
        Usuario existing = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
        if (!currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo SUPER_ADMIN puede archivar usuarios");
        }
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
        dto.setConfirmado(e.getConfirmado());
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
        if (dto.getPersonaId() != null) e.setPersona(findPersona(dto.getPersonaId()));
        if (dto.getEstadoId() != null) e.setEstado(findEstado(dto.getEstadoId()));
        if (dto.getEmpresaId() != null) e.setEmpresa(findEmpresa(dto.getEmpresaId()));
        return e;
    }

    private Persona findPersona(Long id) { return personaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Persona", id)); }
    private Estado findEstado(Long id) { return estadoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estado", id)); }
    private Empresa findEmpresa(Long id) { return empresaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Empresa", id)); }
}
