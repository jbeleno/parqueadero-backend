package com.usco.parqueaderos_api.catalog.service;

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

    // ---- Estado ----
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
    public List<TipoParqueadero> findAllTiposParqueadero() { return tipoParqueaderoRepository.findAll(); }

    @Transactional(readOnly = true)
    public TipoParqueadero findTipoParqueaderoById(Long id) {
        return tipoParqueaderoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoParqueadero", id));
    }

    @Transactional
    public TipoParqueadero saveTipoParqueadero(TipoParqueadero tp) {
        if (tp.getEstado() != null && tp.getEstado().getId() != null) {
            tp.setEstado(findEstadoById(tp.getEstado().getId()));
        }
        return tipoParqueaderoRepository.save(tp);
    }

    @Transactional
    public TipoParqueadero updateTipoParqueadero(Long id, TipoParqueadero tp) {
        TipoParqueadero existing = findTipoParqueaderoById(id);
        existing.setNombre(tp.getNombre());
        existing.setDescripcion(tp.getDescripcion());
        if (tp.getEstado() != null && tp.getEstado().getId() != null) {
            existing.setEstado(findEstadoById(tp.getEstado().getId()));
        }
        return tipoParqueaderoRepository.save(existing);
    }

    @Transactional
    public void deleteTipoParqueadero(Long id) {
        findTipoParqueaderoById(id);
        tipoParqueaderoRepository.deleteById(id);
    }

    // ---- TipoPuntoParqueo ----
    @Transactional(readOnly = true)
    public List<TipoPuntoParqueo> findAllTiposPuntoParqueo() { return tipoPuntoParqueoRepository.findAll(); }

    @Transactional(readOnly = true)
    public TipoPuntoParqueo findTipoPuntoParqueoById(Long id) {
        return tipoPuntoParqueoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoPuntoParqueo", id));
    }

    @Transactional
    public TipoPuntoParqueo saveTipoPuntoParqueo(TipoPuntoParqueo tp) {
        if (tp.getEstado() != null && tp.getEstado().getId() != null) {
            tp.setEstado(findEstadoById(tp.getEstado().getId()));
        }
        return tipoPuntoParqueoRepository.save(tp);
    }

    @Transactional
    public TipoPuntoParqueo updateTipoPuntoParqueo(Long id, TipoPuntoParqueo tp) {
        TipoPuntoParqueo existing = findTipoPuntoParqueoById(id);
        existing.setNombre(tp.getNombre());
        existing.setDescripcion(tp.getDescripcion());
        if (tp.getEstado() != null && tp.getEstado().getId() != null) {
            existing.setEstado(findEstadoById(tp.getEstado().getId()));
        }
        return tipoPuntoParqueoRepository.save(existing);
    }

    @Transactional
    public void deleteTipoPuntoParqueo(Long id) {
        findTipoPuntoParqueoById(id);
        tipoPuntoParqueoRepository.deleteById(id);
    }

    // ---- TipoVehiculo ----
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
    public List<TipoDispositivo> findAllTiposDispositivo() { return tipoDispositivoRepository.findAll(); }

    @Transactional(readOnly = true)
    public TipoDispositivo findTipoDispositivoById(Long id) {
        return tipoDispositivoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoDispositivo", id));
    }

    @Transactional
    public TipoDispositivo saveTipoDispositivo(TipoDispositivo td) {
        if (td.getEstado() != null && td.getEstado().getId() != null) {
            td.setEstado(findEstadoById(td.getEstado().getId()));
        }
        return tipoDispositivoRepository.save(td);
    }

    @Transactional
    public TipoDispositivo updateTipoDispositivo(Long id, TipoDispositivo td) {
        TipoDispositivo existing = findTipoDispositivoById(id);
        existing.setNombre(td.getNombre());
        existing.setDescripcion(td.getDescripcion());
        if (td.getEstado() != null && td.getEstado().getId() != null) {
            existing.setEstado(findEstadoById(td.getEstado().getId()));
        }
        return tipoDispositivoRepository.save(existing);
    }

    @Transactional
    public void deleteTipoDispositivo(Long id) {
        findTipoDispositivoById(id);
        tipoDispositivoRepository.deleteById(id);
    }

    // ---- Rol ----
    @Transactional(readOnly = true)
    public List<Rol> findAllRoles() { return rolRepository.findAll(); }

    @Transactional(readOnly = true)
    public Rol findRolById(Long id) {
        return rolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol", id));
    }

    @Transactional
    public Rol saveRol(Rol rol) {
        if (rol.getEstado() != null && rol.getEstado().getId() != null) {
            rol.setEstado(findEstadoById(rol.getEstado().getId()));
        }
        return rolRepository.save(rol);
    }

    @Transactional
    public Rol updateRol(Long id, Rol rol) {
        Rol existing = findRolById(id);
        existing.setNombre(rol.getNombre());
        existing.setDescripcion(rol.getDescripcion());
        if (rol.getEstado() != null && rol.getEstado().getId() != null) {
            existing.setEstado(findEstadoById(rol.getEstado().getId()));
        }
        return rolRepository.save(existing);
    }

    @Transactional
    public void deleteRol(Long id) {
        findRolById(id);
        rolRepository.deleteById(id);
    }
}
