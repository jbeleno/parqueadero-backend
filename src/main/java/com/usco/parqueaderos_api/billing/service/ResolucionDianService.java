package com.usco.parqueaderos_api.billing.service;

import com.usco.parqueaderos_api.audit.aspect.Auditable;
import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.billing.dto.ResolucionDianDTO;
import com.usco.parqueaderos_api.billing.entity.ResolucionDian;
import com.usco.parqueaderos_api.billing.repository.ResolucionDianRepository;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResolucionDianService {

    private final ResolucionDianRepository repo;
    private final ParqueaderoRepository parqueaderoRepository;
    private final CurrentUserService currentUser;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.usco.parqueaderos_api.user.service.UsuarioNombreResolver nombreResolver;

    /** Valida que el usuario tiene acceso al parqueadero (multi-tenant + assignment). */
    private void validarAccesoParqueadero(Parqueadero p) {
        if (p.getEmpresa() != null) {
            currentUser.requireEmpresa(p.getEmpresa().getId());
        }
        if (currentUser.isAdminParqueadero()) {
            currentUser.requireParqueadero(p.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<ResolucionDianDTO> listarPorParqueadero(Long parqueaderoId) {
        Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        validarAccesoParqueadero(p);
        return repo.findNoArchivadasPorParqueadero(parqueaderoId).stream()
                .map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public ResolucionDianDTO findPrincipal(Long parqueaderoId) {
        Parqueadero p = parqueaderoRepository.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        validarAccesoParqueadero(p);
        return repo.findPrincipalDeParqueadero(parqueaderoId)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ResolucionPrincipal", parqueaderoId));
    }

    @Transactional(readOnly = true)
    public ResolucionDianDTO findById(Long id) {
        ResolucionDian r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResolucionDian", id));
        validarAccesoParqueadero(r.getParqueadero());
        return toDTO(r);
    }

    @Transactional
    @Auditable(tabla = "resolucion_dian", accion = "CREATE")
    public ResolucionDianDTO crear(ResolucionDianDTO dto) {
        Parqueadero p = parqueaderoRepository.findById(dto.getParqueaderoId())
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", dto.getParqueaderoId()));
        validarAccesoParqueadero(p);
        validarRango(dto.getRangoInicial(), dto.getRangoFinal());
        validarVigencia(dto.getVigenteDesde(), dto.getVigenteHasta());

        ResolucionDian r = new ResolucionDian();
        copyFields(r, dto);
        r.setParqueadero(p);
        r.setConsecutivoActual(dto.getConsecutivoActual() != null
                ? dto.getConsecutivoActual()
                : Math.max(0, dto.getRangoInicial() - 1));
        r.setCreadoPorUsuarioId(currentUser.getCurrentUserId());
        r.setFechaCreacion(LocalDateTime.now());

        // Si es la PRIMERA del parqueadero, marcar como principal auto.
        boolean esPrimera = !repo.existsByParqueaderoIdAndArchivadaEnIsNull(p.getId());
        if (esPrimera) {
            r.setPrincipal(true);
        } else {
            r.setPrincipal(false);
        }

        try {
            return toDTO(repo.saveAndFlush(r));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new BusinessException(
                    "Ya existe otra resolucion principal activa en este parqueadero",
                    "ERR_RESOLUCION_PRINCIPAL_DUPLICADA");
        }
    }

    @Transactional
    @Auditable(tabla = "resolucion_dian", accion = "UPDATE")
    public ResolucionDianDTO actualizar(Long id, ResolucionDianDTO dto) {
        ResolucionDian r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResolucionDian", id));
        validarAccesoParqueadero(r.getParqueadero());
        if (r.getArchivadaEn() != null) {
            throw new BusinessException(
                    "No se puede editar una resolucion archivada", "ERR_RESOLUCION_ARCHIVADA");
        }
        validarRango(dto.getRangoInicial(), dto.getRangoFinal());
        validarVigencia(dto.getVigenteDesde(), dto.getVigenteHasta());
        copyFields(r, dto);
        return toDTO(repo.save(r));
    }

    @Transactional
    @Auditable(tabla = "resolucion_dian", accion = "MARCAR_PRINCIPAL")
    public ResolucionDianDTO marcarPrincipal(Long id) {
        ResolucionDian r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResolucionDian", id));
        validarAccesoParqueadero(r.getParqueadero());
        if (r.getArchivadaEn() != null) {
            throw new BusinessException(
                    "No se puede marcar como principal una resolucion archivada",
                    "ERR_RESOLUCION_ARCHIVADA");
        }
        if (Boolean.TRUE.equals(r.getPrincipal())) {
            return toDTO(r); // ya es principal
        }
        // Desactivar la anterior principal del mismo parqueadero
        repo.desactivarPrincipalDeParqueadero(r.getParqueadero().getId());
        r.setPrincipal(true);
        return toDTO(repo.save(r));
    }

    @Transactional
    @Auditable(tabla = "resolucion_dian", accion = "ARCHIVAR", requiereMotivo = true)
    public ResolucionDianDTO archivar(Long id, String motivo) {
        if (motivo == null || motivo.trim().length() < com.usco.parqueaderos_api.common.validation.MotivoValidator.DEFAULT_MIN_CHARS) {
            throw new BusinessException(
                    "Motivo obligatorio (min 10 chars)", "ERR_MISSING_FIELDS");
        }
        ResolucionDian r = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ResolucionDian", id));
        validarAccesoParqueadero(r.getParqueadero());
        if (r.getArchivadaEn() != null) return toDTO(r);
        // Si era la principal, des-marcar
        r.setPrincipal(false);
        r.setArchivadaEn(LocalDateTime.now());
        r.setMotivoArchivado(motivo);
        r.setArchivadoPorUsuarioId(currentUser.getCurrentUserId());
        return toDTO(repo.save(r));
    }

    /**
     * Obtiene la resolucion principal del parqueadero y reserva el siguiente
     * consecutivo (auto-incrementa). Usado por AutoFacturaListener al emitir.
     * Lock pesimista garantiza serializacion de consecutivos.
     * Devuelve null si el parqueadero no tiene resolucion principal.
     */
    @Transactional
    public ResolucionDian reservarSiguienteConsecutivo(Long parqueaderoId) {
        return repo.findPrincipalDeParqueadero(parqueaderoId)
                .map(r -> {
                    ResolucionDian locked = repo.findByIdForUpdate(r.getId())
                            .orElse(r);
                    long siguiente = (locked.getConsecutivoActual() == null ? 0L : locked.getConsecutivoActual()) + 1;
                    if (siguiente > locked.getRangoFinal()) {
                        // Resolucion AGOTADA: bloquea emision en lugar de seguir contando
                        throw new BusinessException(
                                "La resolucion DIAN principal del parqueadero esta AGOTADA. "
                                + "Registra una nueva y marca como principal.",
                                "ERR_RESOLUCION_AGOTADA");
                    }
                    locked.setConsecutivoActual(siguiente);
                    return repo.save(locked);
                })
                .orElse(null);
    }

    // ── Helpers ──

    private void validarRango(Long ini, Long fin) {
        if (ini == null || fin == null || ini <= 0 || fin < ini) {
            throw new BusinessException(
                    "Rango invalido: rangoInicial debe ser > 0 y rangoFinal >= rangoInicial",
                    "ERR_RANGO_INVALIDO");
        }
    }

    private void validarVigencia(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new BusinessException(
                    "Vigencia invalida: vigenteHasta debe ser >= vigenteDesde",
                    "ERR_VIGENCIA_INVALIDA");
        }
    }

    private void copyFields(ResolucionDian r, ResolucionDianDTO dto) {
        r.setNumeroResolucion(dto.getNumeroResolucion());
        r.setFechaResolucion(dto.getFechaResolucion());
        r.setTipoResolucion(dto.getTipoResolucion());
        r.setModalidad(dto.getModalidad());
        r.setPrefijo(dto.getPrefijo());
        r.setRangoInicial(dto.getRangoInicial());
        r.setRangoFinal(dto.getRangoFinal());
        r.setVigenteDesde(dto.getVigenteDesde());
        r.setVigenteHasta(dto.getVigenteHasta());
        r.setNombre(dto.getNombre());
        r.setDescripcion(dto.getDescripcion());
        r.setRegimenTributario(dto.getRegimenTributario());
    }

    public ResolucionDianDTO toDTO(ResolucionDian r) {
        ResolucionDianDTO dto = new ResolucionDianDTO();
        dto.setId(r.getId());
        if (r.getParqueadero() != null) {
            dto.setParqueaderoId(r.getParqueadero().getId());
            dto.setParqueaderoNombre(r.getParqueadero().getNombre());
        }
        dto.setNumeroResolucion(r.getNumeroResolucion());
        dto.setFechaResolucion(r.getFechaResolucion());
        dto.setTipoResolucion(r.getTipoResolucion());
        dto.setModalidad(r.getModalidad());
        dto.setPrefijo(r.getPrefijo());
        dto.setRangoInicial(r.getRangoInicial());
        dto.setRangoFinal(r.getRangoFinal());
        dto.setConsecutivoActual(r.getConsecutivoActual());
        dto.setVigenteDesde(r.getVigenteDesde());
        dto.setVigenteHasta(r.getVigenteHasta());
        dto.setNombre(r.getNombre());
        dto.setDescripcion(r.getDescripcion());
        dto.setRegimenTributario(r.getRegimenTributario());
        dto.setPrincipal(r.getPrincipal());
        dto.setEstadoCalculado(calcularEstado(r));
        dto.setCreadoPorUsuarioId(r.getCreadoPorUsuarioId());
        if (nombreResolver != null) {
            dto.setCreadoPorUsuarioNombre(nombreResolver.nombreOf(r.getCreadoPorUsuarioId()));
        }
        dto.setFechaCreacion(r.getFechaCreacion());
        dto.setArchivadaEn(r.getArchivadaEn());
        return dto;
    }

    private String calcularEstado(ResolucionDian r) {
        LocalDate hoy = LocalDate.now();
        long consecutivo = r.getConsecutivoActual() == null ? 0L : r.getConsecutivoActual();
        if (r.getArchivadaEn() != null)      return "ARCHIVADA";
        if (hoy.isBefore(r.getVigenteDesde())) return "NO_INICIADA";
        if (hoy.isAfter(r.getVigenteHasta()))  return "VENCIDA";
        if (consecutivo >= r.getRangoFinal())  return "AGOTADA";
        return "VIGENTE";
    }
}
