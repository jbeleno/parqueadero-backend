package com.usco.parqueaderos_api.user.service;

import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.user.entity.Persona;
import com.usco.parqueaderos_api.user.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;

    @Transactional(readOnly = true)
    public List<Persona> findAll() { return personaRepository.findAll(); }

    @Transactional(readOnly = true)
    public Persona findById(Long id) {
        return personaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Persona", id));
    }

    @Transactional
    public Persona save(Persona persona) { return personaRepository.save(persona); }

    @Transactional
    public Persona update(Long id, Persona persona) {
        Persona existing = findById(id);
        existing.setNombre(persona.getNombre());
        existing.setApellido(persona.getApellido());
        existing.setTelefono(persona.getTelefono());
        existing.setTipoDocumento(persona.getTipoDocumento());
        existing.setNumeroDocumento(persona.getNumeroDocumento());
        return personaRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        personaRepository.deleteById(id);
    }
}
