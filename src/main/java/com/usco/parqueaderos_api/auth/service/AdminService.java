package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.auth.dto.AsignarRolRequest;
import com.usco.parqueaderos_api.auth.dto.UsuarioAdminDTO;
import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.Rol;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.RolRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.entity.UsuarioRol;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import com.usco.parqueaderos_api.user.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;
    private final EstadoRepository estadoRepository;

    @Transactional
    public String asignarRol(Long usuarioId, AsignarRolRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
        Rol rol = rolRepository.findById(request.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol", request.getRolId()));

        boolean yaExiste = usuarioRolRepository.findByUsuarioId(usuarioId).stream()
                .anyMatch(ur -> ur.getRol().getId().equals(request.getRolId()));
        if (yaExiste) {
            throw new BusinessException("El usuario ya tiene asignado el rol: " + rol.getNombre());
        }

        UsuarioRol ur = new UsuarioRol();
        ur.setUsuario(usuario);
        ur.setRol(rol);
        usuarioRolRepository.save(ur);
        return "Rol '" + rol.getNombre() + "' asignado correctamente.";
    }

    @Transactional
    public String quitarRol(Long usuarioId, Long rolId) {
        List<UsuarioRol> roles = usuarioRolRepository.findByUsuarioId(usuarioId);
        UsuarioRol toRemove = roles.stream()
                .filter(ur -> ur.getRol().getId().equals(rolId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("El usuario no tiene asignado ese rol"));
        usuarioRolRepository.delete(toRemove);
        return "Rol removido correctamente.";
    }

    public List<String> getRoles(Long usuarioId) {
        return usuarioRolRepository.findByUsuarioId(usuarioId).stream()
                .map(ur -> ur.getRol().getNombre())
                .toList();
    }

    @Transactional
    public String cambiarEstadoUsuario(Long usuarioId, Long estadoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
        Estado estado = estadoRepository.findById(estadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Estado", estadoId));
        usuario.setEstado(estado);
        usuarioRepository.save(usuario);
        return "Estado del usuario actualizado a: " + estado.getNombre();
    }

    public Page<UsuarioAdminDTO> listarUsuarios(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(this::toAdminDTO);
    }

    private UsuarioAdminDTO toAdminDTO(Usuario u) {
        List<String> roles = usuarioRolRepository.findByUsuarioId(u.getId()).stream()
                .map(ur -> ur.getRol().getNombre())
                .toList();
        String nombreCompleto = u.getPersona() != null
                ? u.getPersona().getNombre() + " " + u.getPersona().getApellido()
                : "";
        String numeroDoc = u.getPersona() != null ? u.getPersona().getNumeroDocumento() : "";
        return UsuarioAdminDTO.builder()
                .id(u.getId())
                .correo(u.getCorreo())
                .nombreCompleto(nombreCompleto)
                .numeroDocumento(numeroDoc)
                .confirmado(u.getConfirmado())
                .estadoNombre(u.getEstado() != null ? u.getEstado().getNombre() : "")
                .roles(roles)
                .fechaCreacion(u.getFechaCreacion())
                .build();
    }
}
