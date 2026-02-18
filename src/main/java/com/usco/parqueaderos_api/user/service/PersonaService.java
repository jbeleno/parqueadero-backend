package com.usco.parqueaderos_api.user.service;

import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.user.dto.PersonaDTO;
import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.user.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;

    @Transactional(readOnly = true)
    public List<PersonaDTO> findAll() {
        return personaRepository.findAll().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PersonaDTO findById(Long id) {
        return personaRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", id));
    }

    @Transactional
    public PersonaDTO save(PersonaDTO dto) {
        return toDTO(personaRepository.save(toEntity(dto)));
    }

    @Transactional
    public PersonaDTO update(Long id, PersonaDTO dto) {
        Persona existing = personaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", id));
        existing.setNombre(dto.getNombre());
        existing.setApellido(dto.getApellido());
        existing.setTelefono(dto.getTelefono());
        existing.setTipoDocumento(dto.getTipoDocumento());
        existing.setNumeroDocumento(dto.getNumeroDocumento());
        return toDTO(personaRepository.save(existing));
    }

    public PersonaDTO toDTO(Persona e) {
        PersonaDTO dto = new PersonaDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setApellido(e.getApellido());
        dto.setTelefono(e.getTelefono());
        dto.setTipoDocumento(e.getTipoDocumento());
        dto.setNumeroDocumento(e.getNumeroDocumento());
        return dto;
    }

    private Persona toEntity(PersonaDTO dto) {
        Persona e = new Persona();
        e.setNombre(dto.getNombre());
        e.setApellido(dto.getApellido());
        e.setTelefono(dto.getTelefono());
        e.setTipoDocumento(dto.getTipoDocumento());
        e.setNumeroDocumento(dto.getNumeroDocumento());
        return e;
    }
}
