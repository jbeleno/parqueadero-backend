package com.usco.parqueaderos_api.catalog.service;

import com.usco.parqueaderos_api.catalog.dto.RolDTO;
import com.usco.parqueaderos_api.catalog.dto.TipoDispositivoDTO;
import com.usco.parqueaderos_api.catalog.dto.TipoParqueaderoDTO;
import com.usco.parqueaderos_api.catalog.dto.TipoPuntoParqueoDTO;
import com.usco.parqueaderos_api.catalog.entity.*;
import com.usco.parqueaderos_api.catalog.repository.*;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final EstadoRepository estadoRepository;
    private final TipoParqueaderoRepository tipoParqueaderoRepository;
    private final TipoPuntoParqueoRepository tipoPuntoParqueoRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final TipoDispositivoRepository tipoDispositivoRepository;
    private final RolRepository rolRepository;

    // ---- Mappers ----

    private RolDTO toRolDTO(Rol r) {
        RolDTO dto = new RolDTO();
        dto.setId(r.getId());
        dto.setNombre(r.getNombre());
        dto.setDescripcion(r.getDescripcion());
        if (r.getEstado() != null) {
            dto.setEstadoId(r.getEstado().getId());
            dto.setEstadoNombre(r.getEstado().getNombre());
        }
        return dto;
    }

    private TipoParqueaderoDTO toTipoParqueaderoDTO(TipoParqueadero tp) {
        TipoParqueaderoDTO dto = new TipoParqueaderoDTO();
        dto.setId(tp.getId());
        dto.setNombre(tp.getNombre());
        dto.setDescripcion(tp.getDescripcion());
        if (tp.getEstado() != null) {
            dto.setEstadoId(tp.getEstado().getId());
            dto.setEstadoNombre(tp.getEstado().getNombre());
        }
        return dto;
    }

    private TipoPuntoParqueoDTO toTipoPuntoParqueoDTO(TipoPuntoParqueo tp) {
        TipoPuntoParqueoDTO dto = new TipoPuntoParqueoDTO();
        dto.setId(tp.getId());
        dto.setNombre(tp.getNombre());
        dto.setDescripcion(tp.getDescripcion());
        if (tp.getEstado() != null) {
            dto.setEstadoId(tp.getEstado().getId());
            dto.setEstadoNombre(tp.getEstado().getNombre());
        }
        return dto;
    }

    private TipoDispositivoDTO toTipoDispositivoDTO(TipoDispositivo td) {
        TipoDispositivoDTO dto = new TipoDispositivoDTO();
        dto.setId(td.getId());
        dto.setNombre(td.getNombre());
        dto.setDescripcion(td.getDescripcion());
        if (td.getEstado() != null) {
            dto.setEstadoId(td.getEstado().getId());
            dto.setEstadoNombre(td.getEstado().getNombre());
        }
        return dto;
    }

    // ---- Estado (sin lazy, se devuelve directo) ----
    @Transactional(readOnly = true)
    public List<Estado> findAllEstados() { return estadoRepository.findAll(); }

    @Transactional(readOnly = true)
    public Estado findEstadoById(Long id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado", id));
    }

    @Transactional
    public Estado saveEstado(Estado estado) { return estadoRepository.save(estado); }

    @Transactional
    public Estado updateEstado(Long id, Estado estado) {
        Estado existing = findEstadoById(id);
        existing.setNombre(estado.getNombre());
        existing.setDescripcion(estado.getDescripcion());
        return estadoRepository.save(existing);
    }

    @Transactional
    public void deleteEstado(Long id) {
        findEstadoById(id);
        estadoRepository.deleteById(id);
    }

    // ---- TipoParqueadero ----
    @Transactional(readOnly = true)
    public List<TipoParqueaderoDTO> findAllTiposParqueadero() {
        return tipoParqueaderoRepository.findAll().stream()
                .map(this::toTipoParqueaderoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoParqueaderoDTO findTipoParqueaderoById(Long id) {
        return toTipoParqueaderoDTO(findTipoParqueaderoEntity(id));
    }

    @Transactional
    public TipoParqueaderoDTO saveTipoParqueadero(TipoParqueadero tp) {
        if (tp.getEstado() != null && tp.getEstado().getId() != null) {
            tp.setEstado(findEstadoById(tp.getEstado().getId()));
        }
        return toTipoParqueaderoDTO(tipoParqueaderoRepository.save(tp));
    }

    @Transactional
    public TipoParqueaderoDTO updateTipoParqueadero(Long id, TipoParqueadero tp) {
        TipoParqueadero existing = findTipoParqueaderoEntity(id);
        existing.setNombre(tp.getNombre());
        existing.setDescripcion(tp.getDescripcion());
        if (tp.getEstado() != null && tp.getEstado().getId() != null) {
            existing.setEstado(findEstadoById(tp.getEstado().getId()));
        }
        return toTipoParqueaderoDTO(tipoParqueaderoRepository.save(existing));
    }

    @Transactional
    public void deleteTipoParqueadero(Long id) {
        findTipoParqueaderoEntity(id);
        tipoParqueaderoRepository.deleteById(id);
    }

    private TipoParqueadero findTipoParqueaderoEntity(Long id) {
        return tipoParqueaderoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoParqueadero", id));
    }

    // ---- TipoPuntoParqueo ----
    @Transactional(readOnly = true)
    public List<TipoPuntoParqueoDTO> findAllTiposPuntoParqueo() {
        return tipoPuntoParqueoRepository.findAll().stream()
                .map(this::toTipoPuntoParqueoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoPuntoParqueoDTO findTipoPuntoParqueoById(Long id) {
        return toTipoPuntoParqueoDTO(findTipoPuntoParqueoEntity(id));
    }

    @Transactional
    public TipoPuntoParqueoDTO saveTipoPuntoParqueo(TipoPuntoParqueo tp) {
        if (tp.getEstado() != null && tp.getEstado().getId() != null) {
            tp.setEstado(findEstadoById(tp.getEstado().getId()));
        }
        return toTipoPuntoParqueoDTO(tipoPuntoParqueoRepository.save(tp));
    }

    @Transactional
    public TipoPuntoParqueoDTO updateTipoPuntoParqueo(Long id, TipoPuntoParqueo tp) {
        TipoPuntoParqueo existing = findTipoPuntoParqueoEntity(id);
        existing.setNombre(tp.getNombre());
        existing.setDescripcion(tp.getDescripcion());
        if (tp.getEstado() != null && tp.getEstado().getId() != null) {
            existing.setEstado(findEstadoById(tp.getEstado().getId()));
        }
        return toTipoPuntoParqueoDTO(tipoPuntoParqueoRepository.save(existing));
    }

    @Transactional
    public void deleteTipoPuntoParqueo(Long id) {
        findTipoPuntoParqueoEntity(id);
        tipoPuntoParqueoRepository.deleteById(id);
    }

    private TipoPuntoParqueo findTipoPuntoParqueoEntity(Long id) {
        return tipoPuntoParqueoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoPuntoParqueo", id));
    }

    // ---- TipoVehiculo (sin lazy, se devuelve directo) ----
    @Transactional(readOnly = true)
    public List<TipoVehiculo> findAllTiposVehiculo() { return tipoVehiculoRepository.findAll(); }

    @Transactional(readOnly = true)
    public TipoVehiculo findTipoVehiculoById(Long id) {
        return tipoVehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", id));
    }

    @Transactional
    public TipoVehiculo saveTipoVehiculo(TipoVehiculo tv) { return tipoVehiculoRepository.save(tv); }

    @Transactional
    public TipoVehiculo updateTipoVehiculo(Long id, TipoVehiculo tv) {
        TipoVehiculo existing = findTipoVehiculoById(id);
        existing.setNombre(tv.getNombre());
        existing.setDescripcion(tv.getDescripcion());
        return tipoVehiculoRepository.save(existing);
    }

    @Transactional
    public void deleteTipoVehiculo(Long id) {
        findTipoVehiculoById(id);
        tipoVehiculoRepository.deleteById(id);
    }

    // ---- TipoDispositivo ----
    @Transactional(readOnly = true)
    public List<TipoDispositivoDTO> findAllTiposDispositivo() {
        return tipoDispositivoRepository.findAll().stream()
                .map(this::toTipoDispositivoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoDispositivoDTO findTipoDispositivoById(Long id) {
        return toTipoDispositivoDTO(findTipoDispositivoEntity(id));
    }

    @Transactional
    public TipoDispositivoDTO saveTipoDispositivo(TipoDispositivo td) {
        if (td.getEstado() != null && td.getEstado().getId() != null) {
            td.setEstado(findEstadoById(td.getEstado().getId()));
        }
        return toTipoDispositivoDTO(tipoDispositivoRepository.save(td));
    }

    @Transactional
    public TipoDispositivoDTO updateTipoDispositivo(Long id, TipoDispositivo td) {
        TipoDispositivo existing = findTipoDispositivoEntity(id);
        existing.setNombre(td.getNombre());
        existing.setDescripcion(td.getDescripcion());
        if (td.getEstado() != null && td.getEstado().getId() != null) {
            existing.setEstado(findEstadoById(td.getEstado().getId()));
        }
        return toTipoDispositivoDTO(tipoDispositivoRepository.save(existing));
    }

    @Transactional
    public void deleteTipoDispositivo(Long id) {
        findTipoDispositivoEntity(id);
        tipoDispositivoRepository.deleteById(id);
    }

    private TipoDispositivo findTipoDispositivoEntity(Long id) {
        return tipoDispositivoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoDispositivo", id));
    }

    // ---- Rol ----
    @Transactional(readOnly = true)
    public List<RolDTO> findAllRoles() {
        return rolRepository.findAll().stream()
                .map(this::toRolDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolDTO findRolById(Long id) {
        return toRolDTO(findRolEntity(id));
    }

    @Transactional
    public RolDTO saveRol(Rol rol) {
        if (rol.getEstado() != null && rol.getEstado().getId() != null) {
            rol.setEstado(findEstadoById(rol.getEstado().getId()));
        }
        return toRolDTO(rolRepository.save(rol));
    }

    @Transactional
    public RolDTO updateRol(Long id, Rol rol) {
        Rol existing = findRolEntity(id);
        existing.setNombre(rol.getNombre());
        existing.setDescripcion(rol.getDescripcion());
        if (rol.getEstado() != null && rol.getEstado().getId() != null) {
            existing.setEstado(findEstadoById(rol.getEstado().getId()));
        }
        return toRolDTO(rolRepository.save(existing));
    }

    @Transactional
    public void deleteRol(Long id) {
        findRolEntity(id);
        rolRepository.deleteById(id);
    }

    private Rol findRolEntity(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id));
    }
}
