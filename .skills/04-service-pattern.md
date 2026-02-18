# Skill 04: Patrón de Service

## Regla
TODO controller debe inyectar un Service, NUNCA un Repository. Los Services contienen toda la lógica de negocio y la conversión DTO ↔ Entity.

---

## Patrón completo: Service con DTO (para entidades con FKs complejas)

```java
package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.parking.dto.ParqueaderoDTO;
import com.usco.parqueaderos_api.parking.entity.Parqueadero;
import com.usco.parqueaderos_api.parking.repository.ParqueaderoRepository;
import com.usco.parqueaderos_api.location.repository.CiudadRepository;
import com.usco.parqueaderos_api.parking.repository.EmpresaRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoParqueaderoRepository;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParqueaderoService {
    
    private final ParqueaderoRepository parqueaderoRepository;
    private final CiudadRepository ciudadRepository;
    private final EmpresaRepository empresaRepository;
    private final TipoParqueaderoRepository tipoParqueaderoRepository;
    private final EstadoRepository estadoRepository;
    
    @Transactional(readOnly = true)
    public List<ParqueaderoDTO> findAll() {
        return parqueaderoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ParqueaderoDTO findById(Long id) {
        return parqueaderoRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
    }
    
    @Transactional
    public ParqueaderoDTO create(ParqueaderoDTO dto) {
        Parqueadero entity = toEntity(dto);
        return toDTO(parqueaderoRepository.save(entity));
    }
    
    @Transactional
    public ParqueaderoDTO update(Long id, ParqueaderoDTO dto) {
        Parqueadero entity = parqueaderoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
        
        // Actualizar campos propios
        entity.setNombre(dto.getNombre());
        entity.setDireccion(dto.getDireccion());
        entity.setTelefono(dto.getTelefono());
        entity.setLatitud(dto.getLatitud());
        entity.setLongitud(dto.getLongitud());
        entity.setNumeroPuntosParqueo(dto.getNumeroPuntosParqueo());
        entity.setTiempoGraciaMinutos(dto.getTiempoGraciaMinutos());
        entity.setModoCobro(dto.getModoCobro());
        
        if (dto.getHoraInicio() != null) {
            entity.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
        }
        if (dto.getHoraFin() != null) {
            entity.setHoraFin(LocalTime.parse(dto.getHoraFin()));
        }
        
        // Actualizar FKs si cambiaron
        if (dto.getCiudadId() != null) {
            entity.setCiudad(ciudadRepository.findById(dto.getCiudadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ciudad", dto.getCiudadId())));
        }
        if (dto.getEmpresaId() != null) {
            entity.setEmpresa(empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa", dto.getEmpresaId())));
        }
        if (dto.getTipoParqueaderoId() != null) {
            entity.setTipoParqueadero(tipoParqueaderoRepository.findById(dto.getTipoParqueaderoId())
                    .orElseThrow(() -> new ResourceNotFoundException("TipoParqueadero", dto.getTipoParqueaderoId())));
        }
        if (dto.getEstadoId() != null) {
            entity.setEstado(estadoRepository.findById(dto.getEstadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estado", dto.getEstadoId())));
        }
        
        return toDTO(parqueaderoRepository.save(entity));
    }
    
    @Transactional
    public void delete(Long id) {
        if (!parqueaderoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Parqueadero", id);
        }
        parqueaderoRepository.deleteById(id);
    }
    
    // ========== Conversiones ==========
    
    private ParqueaderoDTO toDTO(Parqueadero entity) {
        ParqueaderoDTO dto = new ParqueaderoDTO();
        dto.setId(entity.getId());
        dto.setNombre(entity.getNombre());
        dto.setDireccion(entity.getDireccion());
        dto.setTelefono(entity.getTelefono());
        dto.setLatitud(entity.getLatitud());
        dto.setLongitud(entity.getLongitud());
        dto.setNumeroPuntosParqueo(entity.getNumeroPuntosParqueo());
        dto.setTiempoGraciaMinutos(entity.getTiempoGraciaMinutos());
        dto.setModoCobro(entity.getModoCobro());
        
        if (entity.getHoraInicio() != null) {
            dto.setHoraInicio(entity.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (entity.getHoraFin() != null) {
            dto.setHoraFin(entity.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        
        // Aplanar FKs
        if (entity.getCiudad() != null) {
            dto.setCiudadId(entity.getCiudad().getId());
            dto.setCiudadNombre(entity.getCiudad().getNombre());
        }
        if (entity.getEmpresa() != null) {
            dto.setEmpresaId(entity.getEmpresa().getId());
            dto.setEmpresaNombre(entity.getEmpresa().getNombre());
        }
        if (entity.getTipoParqueadero() != null) {
            dto.setTipoParqueaderoId(entity.getTipoParqueadero().getId());
            dto.setTipoParqueaderoNombre(entity.getTipoParqueadero().getNombre());
        }
        if (entity.getEstado() != null) {
            dto.setEstadoId(entity.getEstado().getId());
            dto.setEstadoNombre(entity.getEstado().getNombre());
        }
        
        return dto;
    }
    
    private Parqueadero toEntity(ParqueaderoDTO dto) {
        Parqueadero entity = new Parqueadero();
        entity.setNombre(dto.getNombre());
        entity.setDireccion(dto.getDireccion());
        entity.setTelefono(dto.getTelefono());
        entity.setLatitud(dto.getLatitud());
        entity.setLongitud(dto.getLongitud());
        entity.setNumeroPuntosParqueo(dto.getNumeroPuntosParqueo());
        entity.setTiempoGraciaMinutos(dto.getTiempoGraciaMinutos());
        entity.setModoCobro(dto.getModoCobro());
        
        if (dto.getHoraInicio() != null) {
            entity.setHoraInicio(LocalTime.parse(dto.getHoraInicio()));
        }
        if (dto.getHoraFin() != null) {
            entity.setHoraFin(LocalTime.parse(dto.getHoraFin()));
        }
        
        // CRÍTICO: Cargar FKs desde repos
        if (dto.getCiudadId() != null) {
            entity.setCiudad(ciudadRepository.findById(dto.getCiudadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ciudad", dto.getCiudadId())));
        }
        if (dto.getEmpresaId() != null) {
            entity.setEmpresa(empresaRepository.findById(dto.getEmpresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa", dto.getEmpresaId())));
        }
        if (dto.getTipoParqueaderoId() != null) {
            entity.setTipoParqueadero(tipoParqueaderoRepository.findById(dto.getTipoParqueaderoId())
                    .orElseThrow(() -> new ResourceNotFoundException("TipoParqueadero", dto.getTipoParqueaderoId())));
        }
        if (dto.getEstadoId() != null) {
            entity.setEstado(estadoRepository.findById(dto.getEstadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estado", dto.getEstadoId())));
        }
        
        return entity;
    }
}
```

---

## Patrón simple: Service para catálogos (sin DTO)

Para entidades simples como Estado, TipoVehiculo, Pais, etc. que no necesitan DTO:

```java
package com.usco.parqueaderos_api.catalog.service;

import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {
    
    private final EstadoRepository estadoRepository;
    // ... otros repos de catálogo
    
    // ===== ESTADOS =====
    
    @Transactional(readOnly = true)
    public List<Estado> findAllEstados() {
        return estadoRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Estado findEstadoById(Long id) {
        return estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado", id));
    }
    
    @Transactional
    public Estado createEstado(Estado estado) {
        return estadoRepository.save(estado);
    }
    
    @Transactional
    public Estado updateEstado(Long id, Estado estado) {
        Estado existing = estadoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estado", id));
        existing.setNombre(estado.getNombre());
        existing.setDescripcion(estado.getDescripcion());
        return estadoRepository.save(existing);
    }
    
    @Transactional
    public void deleteEstado(Long id) {
        if (!estadoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Estado", id);
        }
        estadoRepository.deleteById(id);
    }
    
    // ===== Repetir para TipoParqueadero, TipoVehiculo, etc. =====
}
```

---

## Reglas inquebrantables para Services

1. **SIEMPRE** `@Service` y `@RequiredArgsConstructor`
2. **SIEMPRE** `@Transactional(readOnly = true)` en lecturas, `@Transactional` en escrituras
3. **SIEMPRE** lanzar `ResourceNotFoundException` cuando `findById` retorna vacío
4. **SIEMPRE** cargar FKs desde repos en `toEntity()` — NUNCA dejar FKs nulas
5. **SIEMPRE** verificar existencia antes de `deleteById()`
6. **NUNCA** capturar excepciones en el service — dejar que el `GlobalExceptionHandler` las maneje
7. Los métodos de conversión se llaman `toDTO()` y `toEntity()` (privados)
8. Para `update()`: buscar la entidad existente, setear campos, y llamar `save()` — NO crear entidad nueva
