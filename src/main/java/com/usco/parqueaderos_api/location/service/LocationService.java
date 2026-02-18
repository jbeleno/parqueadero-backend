package com.usco.parqueaderos_api.location.service;

import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.location.entity.Ciudad;
import com.usco.parqueaderos_api.location.entity.Departamento;
import com.usco.parqueaderos_api.location.entity.Pais;
import com.usco.parqueaderos_api.location.repository.CiudadRepository;
import com.usco.parqueaderos_api.location.repository.DepartamentoRepository;
import com.usco.parqueaderos_api.location.repository.PaisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final PaisRepository paisRepository;
    private final DepartamentoRepository departamentoRepository;
    private final CiudadRepository ciudadRepository;

    // ---- Pais ----
    @Transactional(readOnly = true)
    public List<Pais> findAllPaises() { return paisRepository.findAll(); }

    @Transactional(readOnly = true)
    public Pais findPaisById(Long id) {
        return paisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pais", id));
    }

    @Transactional
    public Pais savePais(Pais pais) { return paisRepository.save(pais); }

    @Transactional
    public Pais updatePais(Long id, Pais pais) {
        Pais existing = findPaisById(id);
        existing.setNombre(pais.getNombre());
        existing.setAcronimo(pais.getAcronimo());
        existing.setIdentificadorInternacional(pais.getIdentificadorInternacional());
        return paisRepository.save(existing);
    }

    @Transactional
    public void deletePais(Long id) {
        findPaisById(id);
        paisRepository.deleteById(id);
    }

    // ---- Departamento ----
    @Transactional(readOnly = true)
    public List<Departamento> findAllDepartamentos() { return departamentoRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<Departamento> findDepartamentosByPais(Long paisId) {
        return departamentoRepository.findByPaisId(paisId);
    }

    @Transactional(readOnly = true)
    public Departamento findDepartamentoById(Long id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", id));
    }

    @Transactional
    public Departamento saveDepartamento(Departamento departamento) {
        if (departamento.getPais() != null && departamento.getPais().getId() != null) {
            departamento.setPais(findPaisById(departamento.getPais().getId()));
        }
        return departamentoRepository.save(departamento);
    }

    @Transactional
    public Departamento updateDepartamento(Long id, Departamento departamento) {
        Departamento existing = findDepartamentoById(id);
        existing.setNombre(departamento.getNombre());
        existing.setIdentificadorNacional(departamento.getIdentificadorNacional());
        if (departamento.getPais() != null && departamento.getPais().getId() != null) {
            existing.setPais(findPaisById(departamento.getPais().getId()));
        }
        return departamentoRepository.save(existing);
    }

    @Transactional
    public void deleteDepartamento(Long id) {
        findDepartamentoById(id);
        departamentoRepository.deleteById(id);
    }

    // ---- Ciudad ----
    @Transactional(readOnly = true)
    public List<Ciudad> findAllCiudades() { return ciudadRepository.findAll(); }

    @Transactional(readOnly = true)
    public List<Ciudad> findCiudadesByDepartamento(Long departamentoId) {
        return ciudadRepository.findByDepartamentoId(departamentoId);
    }

    @Transactional(readOnly = true)
    public Ciudad findCiudadById(Long id) {
        return ciudadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ciudad", id));
    }

    @Transactional
    public Ciudad saveCiudad(Ciudad ciudad) {
        if (ciudad.getDepartamento() != null && ciudad.getDepartamento().getId() != null) {
            ciudad.setDepartamento(findDepartamentoById(ciudad.getDepartamento().getId()));
        }
        return ciudadRepository.save(ciudad);
    }

    @Transactional
    public Ciudad updateCiudad(Long id, Ciudad ciudad) {
        Ciudad existing = findCiudadById(id);
        existing.setNombre(ciudad.getNombre());
        existing.setIdentificadorDepartamental(ciudad.getIdentificadorDepartamental());
        if (ciudad.getDepartamento() != null && ciudad.getDepartamento().getId() != null) {
            existing.setDepartamento(findDepartamentoById(ciudad.getDepartamento().getId()));
        }
        return ciudadRepository.save(existing);
    }

    @Transactional
    public void deleteCiudad(Long id) {
        findCiudadById(id);
        ciudadRepository.deleteById(id);
    }
}
