package com.usco.parqueaderos_api.parking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoParqueadero;
import com.usco.parqueaderos_api.catalog.entity.TipoPuntoParqueo;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoParqueaderoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoPuntoParqueoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.location.entity.Ciudad;
import com.usco.parqueaderos_api.location.repository.CiudadRepository;
import com.usco.parqueaderos_api.parking.dto.config.*;
import com.usco.parqueaderos_api.parking.entity.*;
import com.usco.parqueaderos_api.parking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio maestro de configuración de parqueaderos.
 * Maneja el guardado/lectura del JSON completo que el frontend usa para dibujar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingConfigService {

    private final ParqueaderoRepository parqueaderoRepo;
    private final NivelRepository nivelRepo;
    private final SeccionRepository seccionRepo;
    private final SubSeccionRepository subSeccionRepo;
    private final PuntoParqueoRepository puntoParqueoRepo;
    private final CaminoRepository caminoRepo;
    private final EmpresaRepository empresaRepo;
    private final CiudadRepository ciudadRepo;
    private final TipoParqueaderoRepository tipoParqueaderoRepo;
    private final TipoPuntoParqueoRepository tipoPuntoParqueoRepo;
    private final EstadoRepository estadoRepo;

    private final ObjectMapper objectMapper;

    // ─── CONSTANTS ──────────────────────────────────────────────────────
    private static final String ARCHIVADO = "ARCHIVADO";
    private static final String ACTIVO    = "ACTIVO";

    // ═══════════════════════════════════════════════════════════════════
    //  GET — Devolver configuración completa para dibujar
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ParkingLotConfigDTO getConfig(Long parqueaderoId) {
        Parqueadero p = parqueaderoRepo.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));

        ParkingLotConfigDTO config = new ParkingLotConfigDTO();
        config.setParkingLot(toParkingLotInfo(p));

        List<Nivel> niveles = nivelRepo.findByParqueaderoIdAndEstadoNombreNot(parqueaderoId, ARCHIVADO);
        List<FloorConfigDTO> floors = new ArrayList<>();

        for (Nivel nivel : niveles) {
            floors.add(buildFloorConfig(nivel, parqueaderoId));
        }
        config.setFloors(floors);
        return config;
    }

    private FloorConfigDTO buildFloorConfig(Nivel nivel, Long parqueaderoId) {
        FloorConfigDTO floor = new FloorConfigDTO();
        floor.setId(nivel.getId().toString());
        floor.setName(nivel.getNombre());

        // Secciones
        List<Seccion> secciones = seccionRepo.findByNivelIdAndEstadoNombreNot(nivel.getId(), ARCHIVADO);
        floor.setSections(secciones.stream().map(this::toSectionConfig).collect(Collectors.toList()));

        // SubSecciones (de todas las secciones del nivel)
        List<Long> seccionIds = secciones.stream().map(Seccion::getId).collect(Collectors.toList());
        List<SubSeccion> subSecciones = seccionIds.isEmpty() ? List.of()
                : subSeccionRepo.findBySeccionIdInAndEstadoNombreNot(seccionIds, ARCHIVADO);
        floor.setSubsections(subSecciones.stream().map(this::toSubsectionConfig).collect(Collectors.toList()));

        // Puntos de parqueo (de todas las subsecciones)
        List<Long> subSeccionIds = subSecciones.stream().map(SubSeccion::getId).collect(Collectors.toList());
        List<PuntoParqueo> puntos = subSeccionIds.isEmpty() ? List.of()
                : puntoParqueoRepo.findBySubSeccionIdInAndEstadoNombreNot(subSeccionIds, ARCHIVADO);
        floor.setParkingSpots(puntos.stream().map(this::toParkingSpotConfig).collect(Collectors.toList()));

        // Caminos
        List<Camino> caminos = caminoRepo.findByNivelIdAndEstadoNombreNot(nivel.getId(), ARCHIVADO);
        floor.setPaths(caminos.stream().map(this::toPathConfig).collect(Collectors.toList()));

        return floor;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SAVE — Guardar configuración completa (crear o actualizar)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public ParkingLotConfigDTO saveConfig(ParkingLotConfigDTO configDTO) {
        Estado activo = estadoRepo.findByNombreIgnoreCase(ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Estado ACTIVO no encontrado"));

        // 1) Parqueadero
        Parqueadero parqueadero = saveOrUpdateParqueadero(configDTO.getParkingLot(), activo);

        // 2) Pisos (niveles)
        List<FloorConfigDTO> resultFloors = new ArrayList<>();
        if (configDTO.getFloors() != null) {
            for (FloorConfigDTO floorDTO : configDTO.getFloors()) {
                resultFloors.add(saveFloor(floorDTO, parqueadero, activo));
            }
        }

        ParkingLotConfigDTO result = new ParkingLotConfigDTO();
        result.setParkingLot(toParkingLotInfo(parqueadero));
        result.setFloors(resultFloors);
        return result;
    }

    private Parqueadero saveOrUpdateParqueadero(ParkingLotInfoDTO info, Estado activo) {
        Parqueadero p;
        if (info.getId() != null) {
            p = parqueaderoRepo.findById(info.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", info.getId()));
        } else {
            p = new Parqueadero();
            p.setEstado(activo);
        }

        p.setNombre(info.getName());
        p.setDireccion(info.getDireccion());
        p.setTelefono(info.getTelefono());
        p.setLatitud(info.getLatitud());
        p.setLongitud(info.getLongitud());
        p.setNumeroPuntosParqueo(info.getNumeroPuntosParqueo());
        p.setZonaHoraria(info.getZonaHoraria());
        p.setTiempoGraciaMinutos(info.getTiempoGraciaMinutos());
        p.setModoCobro(info.getModoCobro());

        if (info.getHoraInicio() != null) p.setHoraInicio(LocalTime.parse(info.getHoraInicio()));
        if (info.getHoraFin() != null)    p.setHoraFin(LocalTime.parse(info.getHoraFin()));

        if (info.getEmpresaId() != null)
            p.setEmpresa(empresaRepo.findById(info.getEmpresaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa", info.getEmpresaId())));
        if (info.getCiudadId() != null)
            p.setCiudad(ciudadRepo.findById(info.getCiudadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ciudad", info.getCiudadId())));
        if (info.getTipoParqueaderoId() != null)
            p.setTipoParqueadero(tipoParqueaderoRepo.findById(info.getTipoParqueaderoId())
                    .orElseThrow(() -> new ResourceNotFoundException("TipoParqueadero", info.getTipoParqueaderoId())));
        if (info.getEstadoId() != null)
            p.setEstado(estadoRepo.findById(info.getEstadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Estado", info.getEstadoId())));

        return parqueaderoRepo.save(p);
    }

    private FloorConfigDTO saveFloor(FloorConfigDTO dto, Parqueadero parqueadero, Estado activo) {
        // ── Nivel ──
        Nivel nivel = resolveOrCreate(dto.getId(), nivelRepo, "Nivel", () -> {
            Nivel n = new Nivel();
            n.setParqueadero(parqueadero);
            n.setEstado(activo);
            return n;
        });
        nivel.setNombre(dto.getName());
        nivel.setParqueadero(parqueadero);
        nivel = nivelRepo.save(nivel);
        Long nivelId = nivel.getId();

        // Map para traducir refIds del frontend → DB ids
        Map<String, Long> sectionRefMap = new HashMap<>();
        Map<String, Long> subsectionRefMap = new HashMap<>();

        // ── Secciones ──
        List<SectionConfigDTO> savedSections = new ArrayList<>();
        if (dto.getSections() != null) {
            for (SectionConfigDTO secDTO : dto.getSections()) {
                Seccion sec = resolveOrCreate(secDTO.getId(), seccionRepo, "Seccion", () -> {
                    Seccion s = new Seccion();
                    s.setParqueadero(parqueadero);
                    s.setNivel(nivelRepo.getReferenceById(nivelId));
                    s.setEstado(activo);
                    return s;
                });
                sec.setNombre(secDTO.getName());
                sec.setDescripcion(secDTO.getDescription());
                sec.setAcronimo(secDTO.getAcronym());
                sec.setCoordenadas(serializeJson(secDTO.getCoordinates()));
                sec.setParqueadero(parqueadero);
                sec.setNivel(nivelRepo.getReferenceById(nivelId));
                sec = seccionRepo.save(sec);

                sectionRefMap.put(secDTO.getId(), sec.getId());
                savedSections.add(toSectionConfig(sec));
            }
        }

        // ── SubSecciones ──
        List<SubsectionConfigDTO> savedSubsections = new ArrayList<>();
        if (dto.getSubsections() != null) {
            for (SubsectionConfigDTO ssDTO : dto.getSubsections()) {
                // Resolver sección padre
                Long parentSeccionId = resolveRef(ssDTO.getParentSectionId(), sectionRefMap, "parentSectionId");
                Seccion parentSeccion = seccionRepo.getReferenceById(parentSeccionId);

                SubSeccion ss = resolveOrCreate(ssDTO.getId(), subSeccionRepo, "SubSeccion", () -> {
                    SubSeccion sub = new SubSeccion();
                    sub.setEstado(activo);
                    return sub;
                });
                ss.setNombre(ssDTO.getName());
                ss.setDescripcion(ssDTO.getDescription());
                ss.setAcronimo(ssDTO.getAcronym());
                ss.setCoordenadas(serializeJson(ssDTO.getCoordinates()));
                ss.setCantidadPuntos(ssDTO.getParkingSpots());
                ss.setSeccion(parentSeccion);
                ss = subSeccionRepo.save(ss);

                subsectionRefMap.put(ssDTO.getId(), ss.getId());
                savedSubsections.add(toSubsectionConfig(ss));
            }
        }

        // ── Puntos de parqueo ──
        List<ParkingSpotConfigDTO> savedSpots = new ArrayList<>();
        if (dto.getParkingSpots() != null) {
            for (ParkingSpotConfigDTO spotDTO : dto.getParkingSpots()) {
                Long parentSubSeccionId = resolveRef(spotDTO.getSubsectionId(), subsectionRefMap, "subsectionId");
                SubSeccion parentSub = subSeccionRepo.getReferenceById(parentSubSeccionId);

                PuntoParqueo pp = resolveOrCreate(spotDTO.getId(), puntoParqueoRepo, "PuntoParqueo", () -> {
                    PuntoParqueo punto = new PuntoParqueo();
                    punto.setEstado(activo);
                    return punto;
                });
                pp.setNombre(spotDTO.getAcronym() != null ? spotDTO.getAcronym() : "Punto");
                pp.setAcronimo(spotDTO.getAcronym());
                pp.setDescripcion(spotDTO.getDescription());
                pp.setCoordenadas(serializeJson(spotDTO.getCoordinates()));
                pp.setSubSeccion(parentSub);

                // Tipo punto parqueo — buscar por nombre o usar default
                if (spotDTO.getType() != null) {
                    tipoPuntoParqueoRepo.findByNombreIgnoreCase(spotDTO.getType())
                            .ifPresent(pp::setTipoPuntoParqueo);
                }
                if (pp.getTipoPuntoParqueo() == null) {
                    // Asignar el primer tipo disponible como default
                    tipoPuntoParqueoRepo.findAll().stream().findFirst()
                            .ifPresent(pp::setTipoPuntoParqueo);
                }

                pp = puntoParqueoRepo.save(pp);
                savedSpots.add(toParkingSpotConfig(pp));
            }
        }

        // ── Caminos (paths) ──
        List<PathConfigDTO> savedPaths = new ArrayList<>();
        if (dto.getPaths() != null) {
            for (PathConfigDTO pathDTO : dto.getPaths()) {
                Camino camino = resolveOrCreate(pathDTO.getId(), caminoRepo, "Camino", () -> {
                    Camino c = new Camino();
                    c.setNivel(nivelRepo.getReferenceById(nivelId));
                    c.setEstado(activo);
                    return c;
                });
                camino.setTipo(pathDTO.getType());
                camino.setCoordenadas(serializeJson(pathDTO.getCoordinates()));
                camino.setNivel(nivelRepo.getReferenceById(nivelId));
                camino = caminoRepo.save(camino);
                savedPaths.add(toPathConfig(camino));
            }
        }

        FloorConfigDTO result = new FloorConfigDTO();
        result.setId(nivel.getId().toString());
        result.setName(nivel.getNombre());
        result.setSections(savedSections);
        result.setSubsections(savedSubsections);
        result.setParkingSpots(savedSpots);
        result.setPaths(savedPaths);
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ARCHIVAR — Soft-delete del parqueadero y toda su configuración
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public void archivarParqueadero(Long parqueaderoId) {
        Parqueadero p = parqueaderoRepo.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        Estado archivado = estadoRepo.findByNombreIgnoreCase(ARCHIVADO)
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));

        // Archivar niveles y todos los hijos
        List<Nivel> niveles = nivelRepo.findByParqueaderoId(parqueaderoId);
        for (Nivel nivel : niveles) {
            archivarNivelCascade(nivel, archivado);
        }

        p.setEstado(archivado);
        parqueaderoRepo.save(p);
    }

    @Transactional
    public void archivarNivel(Long nivelId) {
        Nivel nivel = nivelRepo.findById(nivelId)
                .orElseThrow(() -> new ResourceNotFoundException("Nivel", nivelId));
        Estado archivado = estadoRepo.findByNombreIgnoreCase(ARCHIVADO)
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        archivarNivelCascade(nivel, archivado);
    }

    private void archivarNivelCascade(Nivel nivel, Estado archivado) {
        // Secciones del nivel
        List<Seccion> secciones = seccionRepo.findByNivelId(nivel.getId());
        List<Long> seccionIds = secciones.stream().map(Seccion::getId).collect(Collectors.toList());

        // SubSecciones
        List<SubSeccion> subSecciones = seccionIds.isEmpty() ? List.of()
                : subSeccionRepo.findBySeccionIdIn(seccionIds);
        List<Long> subSeccionIds = subSecciones.stream().map(SubSeccion::getId).collect(Collectors.toList());

        // Puntos de parqueo
        List<PuntoParqueo> puntos = subSeccionIds.isEmpty() ? List.of()
                : puntoParqueoRepo.findBySubSeccionIdIn(subSeccionIds);
        puntos.forEach(pp -> { pp.setEstado(archivado); puntoParqueoRepo.save(pp); });

        subSecciones.forEach(ss -> { ss.setEstado(archivado); subSeccionRepo.save(ss); });
        secciones.forEach(s -> { s.setEstado(archivado); seccionRepo.save(s); });

        // Caminos
        caminoRepo.findByNivelId(nivel.getId()).forEach(c -> { c.setEstado(archivado); caminoRepo.save(c); });

        nivel.setEstado(archivado);
        nivelRepo.save(nivel);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Entity → Config DTO  mappers
    // ═══════════════════════════════════════════════════════════════════

    private ParkingLotInfoDTO toParkingLotInfo(Parqueadero p) {
        ParkingLotInfoDTO dto = new ParkingLotInfoDTO();
        dto.setId(p.getId());
        dto.setName(p.getNombre());
        dto.setDireccion(p.getDireccion());
        dto.setTelefono(p.getTelefono());
        dto.setLatitud(p.getLatitud());
        dto.setLongitud(p.getLongitud());
        dto.setZonaHoraria(p.getZonaHoraria());
        dto.setTiempoGraciaMinutos(p.getTiempoGraciaMinutos());
        dto.setModoCobro(p.getModoCobro());
        dto.setNumeroPuntosParqueo(p.getNumeroPuntosParqueo());
        if (p.getHoraInicio() != null) dto.setHoraInicio(p.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (p.getHoraFin() != null) dto.setHoraFin(p.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        if (p.getEmpresa() != null)  { dto.setEmpresaId(p.getEmpresa().getId()); dto.setEmpresaNombre(p.getEmpresa().getNombre()); }
        if (p.getCiudad() != null)   { dto.setCiudadId(p.getCiudad().getId()); dto.setCiudadNombre(p.getCiudad().getNombre()); }
        if (p.getTipoParqueadero() != null) { dto.setTipoParqueaderoId(p.getTipoParqueadero().getId()); dto.setTipoParqueaderoNombre(p.getTipoParqueadero().getNombre()); }
        if (p.getEstado() != null) { dto.setEstadoId(p.getEstado().getId()); dto.setEstadoNombre(p.getEstado().getNombre()); }

        // Contar pisos activos
        dto.setNumeroPisos(
            nivelRepo.findByParqueaderoIdAndEstadoNombreNot(p.getId(), ARCHIVADO).size()
        );
        return dto;
    }

    private SectionConfigDTO toSectionConfig(Seccion s) {
        SectionConfigDTO dto = new SectionConfigDTO();
        dto.setId(s.getId().toString());
        dto.setName(s.getNombre());
        dto.setDescription(s.getDescripcion());
        dto.setAcronym(s.getAcronimo());
        dto.setCoordinates(deserializeCoordList(s.getCoordenadas()));
        return dto;
    }

    private SubsectionConfigDTO toSubsectionConfig(SubSeccion ss) {
        SubsectionConfigDTO dto = new SubsectionConfigDTO();
        dto.setId(ss.getId().toString());
        dto.setName(ss.getNombre());
        dto.setDescription(ss.getDescripcion());
        dto.setAcronym(ss.getAcronimo());
        dto.setParentSectionId(ss.getSeccion() != null ? ss.getSeccion().getId().toString() : null);
        dto.setCoordinates(deserializeCoordList(ss.getCoordenadas()));
        dto.setParkingSpots(ss.getCantidadPuntos());
        return dto;
    }

    private ParkingSpotConfigDTO toParkingSpotConfig(PuntoParqueo pp) {
        ParkingSpotConfigDTO dto = new ParkingSpotConfigDTO();
        dto.setId(pp.getId().toString());
        dto.setSubsectionId(pp.getSubSeccion() != null ? pp.getSubSeccion().getId().toString() : null);
        dto.setAcronym(pp.getAcronimo());
        dto.setDescription(pp.getDescripcion());
        dto.setCoordinates(deserializeSpotCoords(pp.getCoordenadas()));
        if (pp.getTipoPuntoParqueo() != null) dto.setType(pp.getTipoPuntoParqueo().getNombre());
        return dto;
    }

    private PathConfigDTO toPathConfig(Camino c) {
        PathConfigDTO dto = new PathConfigDTO();
        dto.setId(c.getId().toString());
        dto.setType(c.getTipo());
        dto.setCoordinates(deserializeCoordList(c.getCoordenadas()));
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Utilidades
    // ═══════════════════════════════════════════════════════════════════

    /** Intenta parsear el id como Long → busca en BD. Si no, crea nuevo con el supplier. */
    @SuppressWarnings("unchecked")
    private <T> T resolveOrCreate(String idStr,
                                   org.springframework.data.jpa.repository.JpaRepository<T, Long> repo,
                                   String entityName,
                                   java.util.function.Supplier<T> newEntitySupplier) {
        if (idStr != null) {
            try {
                Long dbId = Long.parseLong(idStr);
                return repo.findById(dbId).orElse(newEntitySupplier.get());
            } catch (NumberFormatException ignored) {
                // Es un UUID del frontend → nuevo
            }
        }
        return newEntitySupplier.get();
    }

    /** Resuelve una referencia: intenta como Long directo o busca en el refMap. */
    private Long resolveRef(String ref, Map<String, Long> refMap, String fieldName) {
        if (ref == null) throw new IllegalArgumentException(fieldName + " es requerido");

        // Si ya está en el mapa (fue guardado previamente en esta request)
        if (refMap.containsKey(ref)) return refMap.get(ref);

        // Intentar parsear como Long (DB id existente)
        try {
            return Long.parseLong(ref);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + " = '" + ref + "' no se pudo resolver a un ID válido");
        }
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Error serializando coordenadas: {}", e.getMessage());
            return null;
        }
    }

    private List<CoordinateDTO> deserializeCoordList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<CoordinateDTO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando coordenadas: {}", e.getMessage());
            return List.of();
        }
    }

    private ParkingSpotCoordsDTO deserializeSpotCoords(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ParkingSpotCoordsDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando coordenadas de punto: {}", e.getMessage());
            return null;
        }
    }
}
