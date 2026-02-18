package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.entity.Empresa;
import com.usco.parqueaderos_api.parking.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EstadoRepository estadoRepository;

    @Transactional(readOnly = true)
    public List<Empresa> findAll() { return empresaRepository.findAll(); }

    @Transactional(readOnly = true)
    public Empresa findById(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", id));
    }

    @Transactional
    public Empresa save(Empresa empresa) {
        loadFks(empresa);
        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa update(Long id, Empresa empresa) {
        Empresa existing = findById(id);
        existing.setNombre(empresa.getNombre());
        existing.setDescripcion(empresa.getDescripcion());
        if (empresa.getEstado() != null && empresa.getEstado().getId() != null) {
            existing.setEstado(findEstado(empresa.getEstado().getId()));
        }
        return empresaRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        empresaRepository.deleteById(id);
    }

    private void loadFks(Empresa empresa) {
        if (empresa.getEstado() != null && empresa.getEstado().getId() != null) {
            empresa.setEstado(findEstado(empresa.getEstado().getId()));
        }
    }

    private Estado findEstado(Long id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado", id));
    }
}
