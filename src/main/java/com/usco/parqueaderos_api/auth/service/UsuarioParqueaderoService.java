package com.usco.parqueaderos_api.auth.service;

import com.usco.parqueaderos_api.audit.service.AuditService;
import com.usco.parqueaderos_api.auth.entity.UsuarioParqueadero;
import com.usco.parqueaderos_api.auth.repository.UsuarioParqueaderoRepository;
import com.usco.parqueaderos_api.catalog.entity.Rol;
import com.usco.parqueaderos_api.catalog.repository.RolRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.user.entity.Usuario;
import com.usco.parqueaderos_api.user.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Gestion de asignaciones usuario <-> parqueadero <-> rol.
 *
 * Reglas:
 * - SUPER_ADMIN puede asignar/desasignar a cualquier usuario en cualquier parqueadero.
 * - ADMIN puede asignar/desasignar dentro de los parqueaderos de su empresa.
 * - ADMIN_PARQUEADERO puede asignar/desasignar SOLO OPERARIO_CAJA en sus parqueaderos.
 * - OPERARIO_CAJA / USER: no pueden asignar a nadie.
 *
 * Roles asignables aqui: ADMIN_PARQUEADERO, OPERARIO_CAJA.
 * Los roles ADMIN, SUPER_ADMIN, USER se gestionan por usuario_rol y son globales.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioParqueaderoService {

    private static final Set<String> ROLES_ASIGNABLES = Set.of("ADMIN_PARQUEADERO", "OPERARIO_CAJA");

    private final UsuarioParqueaderoRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ParqueaderoRepository parqueaderoRepository;
    private final RolRepository rolRepository;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    @Transactional
    public UsuarioParqueadero asignar(Long usuarioId, Long parqueaderoId, Long rolId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
        Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        Rol r = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", rolId));

        if (!ROLES_ASIGNABLES.contains(r.getNombre())) {
            throw new BusinessException(
                    "Rol no asignable a parqueadero: " + r.getNombre(),
                    "ERR_ROL_NO_ASIGNABLE");
        }

        // RBAC: quien puede asignar?
        validarPuedeGestionar(parqueaderoId, r.getNombre());

        // Idempotencia: si ya existe, reactiva si estaba desactivada
        UsuarioParqueadero.PK pk = new UsuarioParqueadero.PK(usuarioId, parqueaderoId, rolId);
        UsuarioParqueadero existente = repository.findById(pk).orElse(null);
        UsuarioParqueadero saved;
        if (existente != null) {
            existente.setActivo(true);
            existente.setMotivoDesasignacion(null);
            existente.setDesasignadoEn(null);
            saved = repository.save(existente);
        } else {
            UsuarioParqueadero up = new UsuarioParqueadero();
            up.setUsuario(u);
            up.setParqueadero(p);
            up.setRol(r);
            up.setActivo(true);
            up.setAsignadoEn(LocalDateTime.now());
            up.setAsignadoPorUsuarioId(currentUser.getCurrentUserId());
            saved = repository.save(up);
        }

        audit.log("usuario_parqueadero", null, "ASIGNAR",
                null,
                java.util.Map.of(
                        "usuarioId", usuarioId,
                        "parqueaderoId", parqueaderoId,
                        "rol", r.getNombre()),
                p.getEmpresa() != null ? p.getEmpresa().getId() : null);
        log.info("Asignacion: usuario={} parqueadero={} rol={} por={}",
                usuarioId, parqueaderoId, r.getNombre(), currentUser.getCurrentUserId());
        return saved;
    }

    @Transactional
    public void desasignar(Long usuarioId, Long parqueaderoId, Long rolId, String motivo) {
        Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        Rol r = rolRepository.findById(rolId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", rolId));
        validarPuedeGestionar(parqueaderoId, r.getNombre());

        if (motivo == null || motivo.trim().length() < 10) {
            throw new BusinessException(
                    "motivo de desasignacion obligatorio (min 10 chars)",
                    "ERR_MISSING_FIELDS");
        }

        UsuarioParqueadero.PK pk = new UsuarioParqueadero.PK(usuarioId, parqueaderoId, rolId);
        UsuarioParqueadero up = repository.findById(pk)
                .orElseThrow(() -> new ResourceNotFoundException("Asignacion", null));
        up.setActivo(false);
        up.setMotivoDesasignacion(motivo);
        up.setDesasignadoEn(LocalDateTime.now());
        repository.save(up);

        audit.log("usuario_parqueadero", null, "DESASIGNAR",
                java.util.Map.of(
                        "usuarioId", usuarioId,
                        "parqueaderoId", parqueaderoId,
                        "rol", r.getNombre()),
                null,
                p.getEmpresa() != null ? p.getEmpresa().getId() : null,
                motivo);
    }

    @Transactional(readOnly = true)
    public List<UsuarioParqueadero> asignacionesDeUsuario(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    private void validarPuedeGestionar(Long parqueaderoId, String rolDestino) {
        if (currentUser.isSuperAdmin()) return;
        if (currentUser.isAdmin()) {
            // ADMIN puede asignar cualquier rol en sus parqueaderos
            Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
            if (p.getEmpresa() != null) {
                currentUser.requireEmpresa(p.getEmpresa().getId());
            }
            return;
        }
        if (currentUser.isAdminParqueadero()) {
            // ADMIN_PARQUEADERO SOLO puede asignar OPERARIO_CAJA en sus parqueaderos
            if (!"OPERARIO_CAJA".equals(rolDestino)) {
                throw new AccessDeniedException(
                        "ADMIN_PARQUEADERO solo puede asignar OPERARIO_CAJA");
            }
            if (!currentUser.getParqueaderoIds().contains(parqueaderoId)) {
                throw new AccessDeniedException(
                        "Parqueadero no esta en tu asignacion");
            }
            return;
        }
        throw new AccessDeniedException("Sin permisos para gestionar asignaciones");
    }
}
