package com.usco.parqueaderos_api.vehicle.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.entity.TipoVehiculo;
import com.usco.parqueaderos_api.catalog.repository.TipoVehiculoRepository;
import com.usco.parqueaderos_api.common.exception.DuplicateResourceException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
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
    private final CurrentUserService currentUser;

    @Transactional(readOnly = true)
    public List<VehiculoDTO> findAll() {
        return findAll(false);
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
    public List<VehiculoDTO> findAll(boolean soloMiEmpresa) {
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
        return base.stream().map(this::toDTO).collect(Collectors.toList());
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

    @Transactional
    public void delete(Long id) {
        vehiculoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vehiculo", id));
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
