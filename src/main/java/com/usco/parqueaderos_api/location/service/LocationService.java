package com.usco.parqueaderos_api.location.service;

import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.location.dto.CiudadDTO;
import com.usco.parqueaderos_api.location.dto.DepartamentoDTO;
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

    // ---- Mappers ----

    private DepartamentoDTO toDepartamentoDTO(Departamento d) {
        DepartamentoDTO dto = new DepartamentoDTO();
        dto.setId(d.getId());
        dto.setNombre(d.getNombre());
        dto.setIdentificadorNacional(d.getIdentificadorNacional());
        if (d.getPais() != null) {
            dto.setPaisId(d.getPais().getId());
            dto.setPaisNombre(d.getPais().getNombre());
        }
        return dto;
    }

    private CiudadDTO toCiudadDTO(Ciudad c) {
        CiudadDTO dto = new CiudadDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setIdentificadorDepartamental(c.getIdentificadorDepartamental());
        if (c.getDepartamento() != null) {
            dto.setDepartamentoId(c.getDepartamento().getId());
            dto.setDepartamentoNombre(c.getDepartamento().getNombre());
        }
        return dto;
    }

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
    public List<DepartamentoDTO> findAllDepartamentos() {
        return departamentoRepository.findAll().stream()
                .map(this::toDepartamentoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartamentoDTO> findDepartamentosByPais(Long paisId) {
        return departamentoRepository.findByPaisId(paisId).stream()
                .map(this::toDepartamentoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartamentoDTO findDepartamentoById(Long id) {
        return toDepartamentoDTO(findDepartamentoEntity(id));
    }

    @Transactional
    public DepartamentoDTO saveDepartamento(Departamento departamento) {
        if (departamento.getPais() != null && departamento.getPais().getId() != null) {
            departamento.setPais(findPaisById(departamento.getPais().getId()));
        }
        return toDepartamentoDTO(departamentoRepository.save(departamento));
    }

    @Transactional
    public DepartamentoDTO updateDepartamento(Long id, Departamento departamento) {
        Departamento existing = findDepartamentoEntity(id);
        existing.setNombre(departamento.getNombre());
        existing.setIdentificadorNacional(departamento.getIdentificadorNacional());
        if (departamento.getPais() != null && departamento.getPais().getId() != null) {
            existing.setPais(findPaisById(departamento.getPais().getId()));
        }
        return toDepartamentoDTO(departamentoRepository.save(existing));
    }

    @Transactional
    public void deleteDepartamento(Long id) {
        findDepartamentoEntity(id);
        departamentoRepository.deleteById(id);
    }

    private Departamento findDepartamentoEntity(Long id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", id));
    }

    // ---- Ciudad ----
    @Transactional(readOnly = true)
    public List<CiudadDTO> findAllCiudades() {
        return ciudadRepository.findAll().stream()
                .map(this::toCiudadDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CiudadDTO> findCiudadesByDepartamento(Long departamentoId) {
        return ciudadRepository.findByDepartamentoId(departamentoId).stream()
                .map(this::toCiudadDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public CiudadDTO findCiudadById(Long id) {
        return toCiudadDTO(findCiudadEntity(id));
    }

    @Transactional
    public CiudadDTO saveCiudad(Ciudad ciudad) {
        if (ciudad.getDepartamento() != null && ciudad.getDepartamento().getId() != null) {
            ciudad.setDepartamento(findDepartamentoEntity(ciudad.getDepartamento().getId()));
        }
        return toCiudadDTO(ciudadRepository.save(ciudad));
    }

    @Transactional
    public CiudadDTO updateCiudad(Long id, Ciudad ciudad) {
        Ciudad existing = findCiudadEntity(id);
        existing.setNombre(ciudad.getNombre());
        existing.setIdentificadorDepartamental(ciudad.getIdentificadorDepartamental());
        if (ciudad.getDepartamento() != null && ciudad.getDepartamento().getId() != null) {
            existing.setDepartamento(findDepartamentoEntity(ciudad.getDepartamento().getId()));
        }
        return toCiudadDTO(ciudadRepository.save(existing));
    }

    @Transactional
    public void deleteCiudad(Long id) {
        findCiudadEntity(id);
        ciudadRepository.deleteById(id);
    }

    private Ciudad findCiudadEntity(Long id) {
        return ciudadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ciudad", id));
    }
}
