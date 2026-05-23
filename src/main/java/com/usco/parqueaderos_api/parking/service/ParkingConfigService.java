package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.catalog.entity.Estado;
import com.usco.parqueaderos_api.catalog.entity.TipoPuntoParqueo;
import com.usco.parqueaderos_api.catalog.repository.EstadoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoParqueaderoRepository;
import com.usco.parqueaderos_api.catalog.repository.TipoPuntoParqueoRepository;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.location.repository.CiudadRepository;
import com.usco.parqueaderos_api.parking.dto.config.*;
import com.usco.parqueaderos_api.parking.entity.*;
import com.usco.parqueaderos_api.parking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuracion completa del parqueadero (jerarquia: nivel/seccion/subseccion/punto + caminos + camaras).
 *
 * - GET: arma el ParkingLotConfigDTO leyendo BD + delegando mapeo a ParkingConfigMapper.
 * - SAVE: diff estricto contra BD (archiva entidades faltantes) y persiste lo que vino en el DTO.
 * - ARCHIVE: soft-delete cascada de un nivel o parqueadero entero.
 *
 * La serializacion JSON de campos custom (coords, assignedSpots) esta en ParkingConfigCodec.
 * Los mappers entity -> DTO estan en ParkingConfigMapper.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingConfigService {

    private static final String ARCHIVADO = "ARCHIVADO";
    private static final String ACTIVO    = "ACTIVO";

    private final ParqueaderoRepository parqueaderoRepo;
    private final NivelRepository nivelRepo;
    private final SeccionRepository seccionRepo;
    private final SubSeccionRepository subSeccionRepo;
    private final PuntoParqueoRepository puntoParqueoRepo;
    private final CaminoRepository caminoRepo;
    private final CamaraRepository camaraRepo;
    private final EmpresaRepository empresaRepo;
    private final CiudadRepository ciudadRepo;
    private final TipoParqueaderoRepository tipoParqueaderoRepo;
    private final TipoPuntoParqueoRepository tipoPuntoParqueoRepo;
    private final EstadoRepository estadoRepo;

    private final CurrentUserService currentUser;
    private final ParkingConfigCodec codec;
    private final ParkingConfigMapper mapper;

    // ═══════════════════════════════════════════════════════════════════
    //  GET
    // ═══════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ParkingLotConfigDTO getConfig(Long parqueaderoId) {
        Parqueadero p = parqueaderoRepo.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));

        if (currentUser.isAdmin() && !currentUser.isSuperAdmin() && p.getEmpresa() != null) {
            currentUser.requireEmpresa(p.getEmpresa().getId());
        }

        ParkingLotConfigDTO config = new ParkingLotConfigDTO();
        config.setParkingLot(mapper.toParkingLotInfo(p));

        List<FloorConfigDTO> floors = new ArrayList<>();
        for (Nivel nivel : nivelRepo.findByParqueaderoIdAndEstadoNombreNot(parqueaderoId, ARCHIVADO)) {
            floors.add(buildFloorConfig(nivel));
        }
        config.setFloors(floors);
        return config;
    }

    private FloorConfigDTO buildFloorConfig(Nivel nivel) {
        FloorConfigDTO floor = new FloorConfigDTO();
        floor.setId(nivel.getId().toString());
        floor.setName(nivel.getNombre());

        List<Seccion> secciones = seccionRepo.findByNivelIdAndEstadoNombreNot(nivel.getId(), ARCHIVADO);
        floor.setSections(secciones.stream().map(mapper::toSectionConfig).collect(Collectors.toList()));

        List<Long> seccionIds = secciones.stream().map(Seccion::getId).collect(Collectors.toList());
        List<SubSeccion> subSecciones = seccionIds.isEmpty() ? List.of()
                : subSeccionRepo.findBySeccionIdInAndEstadoNombreNot(seccionIds, ARCHIVADO);
        floor.setSubsections(subSecciones.stream().map(mapper::toSubsectionConfig).collect(Collectors.toList()));

        List<Long> subSeccionIds = subSecciones.stream().map(SubSeccion::getId).collect(Collectors.toList());
        List<PuntoParqueo> puntos = subSeccionIds.isEmpty() ? List.of()
                : puntoParqueoRepo.findBySubSeccionIdInAndEstadoNombreNot(subSeccionIds, ARCHIVADO);
        floor.setParkingSpots(puntos.stream().map(mapper::toParkingSpotConfig).collect(Collectors.toList()));

        floor.setPaths(caminoRepo.findByNivelIdAndEstadoNombreNot(nivel.getId(), ARCHIVADO)
                .stream().map(mapper::toPathConfig).collect(Collectors.toList()));

        floor.setCameras(camaraRepo.findByNivelIdAndEstadoNombreNot(nivel.getId(), ARCHIVADO)
                .stream().map(mapper::toCameraConfig).collect(Collectors.toList()));

        return floor;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SAVE (con diff strict: archiva entidades faltantes)
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public ParkingLotConfigDTO saveConfig(ParkingLotConfigDTO configDTO) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo ADMIN o SUPER_ADMIN pueden modificar la configuracion de un parqueadero");
        }

        Estado activo = estadoRepo.findByNombreIgnoreCase(ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Estado ACTIVO no encontrado"));
        Estado archivado = estadoRepo.findByNombreIgnoreCase(ARCHIVADO)
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));

        validarOwnership(configDTO);

        Parqueadero parqueadero = saveOrUpdateParqueadero(configDTO.getParkingLot(), activo);

        // Archivar floors activos que no vienen en el payload (cascade)
        Set<Long> dtoFloorIds = collectDbIds(configDTO.getFloors(), FloorConfigDTO::getId);
        for (Nivel n : nivelRepo.findByParqueaderoIdAndEstadoNombreNot(parqueadero.getId(), ARCHIVADO)) {
            if (!dtoFloorIds.contains(n.getId())) {
                archivarNivelCascade(n, archivado);
            }
        }

        List<FloorConfigDTO> resultFloors = new ArrayList<>();
        if (configDTO.getFloors() != null) {
            for (FloorConfigDTO floorDTO : configDTO.getFloors()) {
                resultFloors.add(saveFloor(floorDTO, parqueadero, activo, archivado));
            }
        }

        ParkingLotConfigDTO result = new ParkingLotConfigDTO();
        result.setParkingLot(mapper.toParkingLotInfo(parqueadero));
        result.setFloors(resultFloors);
        return result;
    }

    private void validarOwnership(ParkingLotConfigDTO configDTO) {
        if (!currentUser.isAdmin() || currentUser.isSuperAdmin()) return;
        if (configDTO.getParkingLot() == null) return;
        Long id = configDTO.getParkingLot().getId();
        if (id != null) {
            Parqueadero existente = parqueaderoRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", id));
            if (existente.getEmpresa() != null) {
                currentUser.requireEmpresa(existente.getEmpresa().getId());
            }
        } else {
            currentUser.getCurrentEmpresaId().ifPresent(configDTO.getParkingLot()::setEmpresaId);
        }
    }

    private Parqueadero saveOrUpdateParqueadero(ParkingLotInfoDTO info, Estado activo) {
        Parqueadero p = (info.getId() != null)
                ? parqueaderoRepo.findById(info.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", info.getId()))
                : nuevoParqueadero(activo);

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

    private Parqueadero nuevoParqueadero(Estado activo) {
        Parqueadero p = new Parqueadero();
        p.setEstado(activo);
        return p;
    }

    private FloorConfigDTO saveFloor(FloorConfigDTO dto, Parqueadero parqueadero,
                                      Estado activo, Estado archivado) {
        Nivel nivel = resolveOrCreate(dto.getId(), nivelRepo, () -> {
            Nivel n = new Nivel();
            n.setParqueadero(parqueadero);
            n.setEstado(activo);
            return n;
        });
        nivel.setNombre(dto.getName());
        nivel.setParqueadero(parqueadero);
        nivel.setEstado(activo);
        nivel = nivelRepo.save(nivel);
        Long nivelId = nivel.getId();

        archivarHijosFaltantes(dto, nivelId, archivado);

        Map<String, Long> sectionRefMap = new HashMap<>();
        Map<String, Long> subsectionRefMap = new HashMap<>();

        List<SectionConfigDTO> savedSections = persistSections(dto, parqueadero, nivelId, activo, sectionRefMap);
        List<SubsectionConfigDTO> savedSubsections = persistSubsections(dto, activo, sectionRefMap, subsectionRefMap);
        List<ParkingSpotConfigDTO> savedSpots = persistSpots(dto, activo, subsectionRefMap);
        List<PathConfigDTO> savedPaths = persistPaths(dto, nivelId, activo);
        List<CameraConfigDTO> savedCameras = persistCameras(dto, nivelId, activo, sectionRefMap);

        FloorConfigDTO result = new FloorConfigDTO();
        result.setId(nivel.getId().toString());
        result.setName(nivel.getNombre());
        result.setSections(savedSections);
        result.setSubsections(savedSubsections);
        result.setParkingSpots(savedSpots);
        result.setPaths(savedPaths);
        result.setCameras(savedCameras);
        return result;
    }

    /** Diff strict por nivel: archiva los hijos activos que no vienen en el payload. */
    private void archivarHijosFaltantes(FloorConfigDTO dto, Long nivelId, Estado archivado) {
        Set<Long> dtoSectionIds    = collectDbIds(dto.getSections(),    SectionConfigDTO::getId);
        Set<Long> dtoSubsectionIds = collectDbIds(dto.getSubsections(), SubsectionConfigDTO::getId);
        Set<Long> dtoSpotIds       = collectDbIds(dto.getParkingSpots(), ParkingSpotConfigDTO::getId);
        Set<Long> dtoPathIds       = collectDbIds(dto.getPaths(),       PathConfigDTO::getId);
        Set<Long> dtoCameraIds     = collectDbIds(dto.getCameras(),     CameraConfigDTO::getId);

        List<Seccion> seccionesActivas = seccionRepo.findByNivelIdAndEstadoNombreNot(nivelId, ARCHIVADO);
        List<Long> seccionActivaIds = seccionesActivas.stream().map(Seccion::getId).collect(Collectors.toList());
        List<SubSeccion> subsActivas = seccionActivaIds.isEmpty() ? List.of()
                : subSeccionRepo.findBySeccionIdInAndEstadoNombreNot(seccionActivaIds, ARCHIVADO);
        List<Long> subActivaIds = subsActivas.stream().map(SubSeccion::getId).collect(Collectors.toList());
        List<PuntoParqueo> puntosActivos = subActivaIds.isEmpty() ? List.of()
                : puntoParqueoRepo.findBySubSeccionIdInAndEstadoNombreNot(subActivaIds, ARCHIVADO);

        // orden: hijos antes que padres
        archivarFaltantes(puntosActivos, PuntoParqueo::getId, dtoSpotIds, archivado, puntoParqueoRepo::save, PuntoParqueo::setEstado);
        archivarFaltantes(subsActivas,    SubSeccion::getId,    dtoSubsectionIds, archivado, subSeccionRepo::save, SubSeccion::setEstado);
        archivarFaltantes(seccionesActivas, Seccion::getId,     dtoSectionIds,    archivado, seccionRepo::save,    Seccion::setEstado);
        archivarFaltantes(caminoRepo.findByNivelIdAndEstadoNombreNot(nivelId, ARCHIVADO),
                Camino::getId, dtoPathIds, archivado, caminoRepo::save, Camino::setEstado);
        archivarFaltantes(camaraRepo.findByNivelIdAndEstadoNombreNot(nivelId, ARCHIVADO),
                Camara::getId, dtoCameraIds, archivado, camaraRepo::save, Camara::setEstado);
    }

    private <T> void archivarFaltantes(List<T> activos,
                                        java.util.function.Function<T, Long> idGetter,
                                        Set<Long> idsEnPayload,
                                        Estado archivado,
                                        java.util.function.Consumer<T> saver,
                                        java.util.function.BiConsumer<T, Estado> setEstado) {
        for (T entity : activos) {
            if (!idsEnPayload.contains(idGetter.apply(entity))) {
                setEstado.accept(entity, archivado);
                saver.accept(entity);
            }
        }
    }

    private List<SectionConfigDTO> persistSections(FloorConfigDTO dto, Parqueadero parqueadero,
                                                    Long nivelId, Estado activo,
                                                    Map<String, Long> sectionRefMap) {
        List<SectionConfigDTO> out = new ArrayList<>();
        if (dto.getSections() == null) return out;
        for (SectionConfigDTO secDTO : dto.getSections()) {
            Seccion sec = resolveOrCreate(secDTO.getId(), seccionRepo, () -> {
                Seccion s = new Seccion();
                s.setParqueadero(parqueadero);
                s.setNivel(nivelRepo.getReferenceById(nivelId));
                s.setEstado(activo);
                return s;
            });
            sec.setNombre(secDTO.getName());
            sec.setDescripcion(secDTO.getDescription());
            sec.setAcronimo(secDTO.getAcronym());
            sec.setCoordenadas(codec.serializeJson(secDTO.getCoordinates()));
            sec.setParqueadero(parqueadero);
            sec.setNivel(nivelRepo.getReferenceById(nivelId));
            sec.setEstado(activo);
            sec = seccionRepo.save(sec);
            sectionRefMap.put(secDTO.getId(), sec.getId());
            out.add(mapper.toSectionConfig(sec));
        }
        return out;
    }

    private List<SubsectionConfigDTO> persistSubsections(FloorConfigDTO dto, Estado activo,
                                                          Map<String, Long> sectionRefMap,
                                                          Map<String, Long> subsectionRefMap) {
        List<SubsectionConfigDTO> out = new ArrayList<>();
        if (dto.getSubsections() == null) return out;
        for (SubsectionConfigDTO ssDTO : dto.getSubsections()) {
            Long parentSeccionId = resolveRef(ssDTO.getParentSectionId(), sectionRefMap, "parentSectionId");
            Seccion parentSeccion = seccionRepo.getReferenceById(parentSeccionId);

            SubSeccion ss = resolveOrCreate(ssDTO.getId(), subSeccionRepo, () -> {
                SubSeccion sub = new SubSeccion();
                sub.setEstado(activo);
                return sub;
            });
            ss.setNombre(ssDTO.getName());
            ss.setDescripcion(ssDTO.getDescription());
            ss.setAcronimo(ssDTO.getAcronym());
            ss.setCoordenadas(codec.serializeJson(ssDTO.getCoordinates()));
            ss.setCantidadPuntos(ssDTO.getParkingSpots());
            ss.setSeccion(parentSeccion);
            ss.setEstado(activo);
            ss = subSeccionRepo.save(ss);
            subsectionRefMap.put(ssDTO.getId(), ss.getId());
            out.add(mapper.toSubsectionConfig(ss));
        }
        return out;
    }

    private List<ParkingSpotConfigDTO> persistSpots(FloorConfigDTO dto, Estado activo,
                                                    Map<String, Long> subsectionRefMap) {
        List<ParkingSpotConfigDTO> out = new ArrayList<>();
        if (dto.getParkingSpots() == null) return out;
        // Cache del default tipo punto: una sola consulta independientemente del numero de spots
        TipoPuntoParqueo defaultTipo = tipoPuntoParqueoRepo.findAll().stream().findFirst().orElse(null);

        for (ParkingSpotConfigDTO spotDTO : dto.getParkingSpots()) {
            Long parentSubSeccionId = resolveRef(spotDTO.getSubsectionId(), subsectionRefMap, "subsectionId");
            SubSeccion parentSub = subSeccionRepo.getReferenceById(parentSubSeccionId);

            PuntoParqueo pp = resolveOrCreate(spotDTO.getId(), puntoParqueoRepo, () -> {
                PuntoParqueo punto = new PuntoParqueo();
                punto.setEstado(activo);
                return punto;
            });
            pp.setNombre(spotDTO.getAcronym() != null ? spotDTO.getAcronym() : "Punto");
            pp.setAcronimo(spotDTO.getAcronym());
            pp.setDescripcion(spotDTO.getDescription());
            pp.setCoordenadas(codec.serializeJson(spotDTO.getCoordinates()));
            pp.setSubSeccion(parentSub);
            pp.setEstado(activo);

            if (spotDTO.getType() != null) {
                tipoPuntoParqueoRepo.findByNombreIgnoreCase(spotDTO.getType())
                        .ifPresent(pp::setTipoPuntoParqueo);
            }
            if (pp.getTipoPuntoParqueo() == null && defaultTipo != null) {
                pp.setTipoPuntoParqueo(defaultTipo);
            }

            pp = puntoParqueoRepo.save(pp);
            out.add(mapper.toParkingSpotConfig(pp));
        }
        return out;
    }

    private List<PathConfigDTO> persistPaths(FloorConfigDTO dto, Long nivelId, Estado activo) {
        List<PathConfigDTO> out = new ArrayList<>();
        if (dto.getPaths() == null) return out;
        for (PathConfigDTO pathDTO : dto.getPaths()) {
            Camino camino = resolveOrCreate(pathDTO.getId(), caminoRepo, () -> {
                Camino c = new Camino();
                c.setNivel(nivelRepo.getReferenceById(nivelId));
                c.setEstado(activo);
                return c;
            });
            camino.setTipo(pathDTO.getType());
            camino.setCoordenadas(codec.serializeJson(pathDTO.getCoordinates()));
            camino.setNivel(nivelRepo.getReferenceById(nivelId));
            camino.setEstado(activo);
            camino = caminoRepo.save(camino);
            out.add(mapper.toPathConfig(camino));
        }
        return out;
    }

    private List<CameraConfigDTO> persistCameras(FloorConfigDTO dto, Long nivelId, Estado activo,
                                                  Map<String, Long> sectionRefMap) {
        List<CameraConfigDTO> out = new ArrayList<>();
        if (dto.getCameras() == null) return out;
        for (CameraConfigDTO camDTO : dto.getCameras()) {
            Camara camara = resolveOrCreate(camDTO.getId(), camaraRepo, () -> {
                Camara c = new Camara();
                c.setNivel(nivelRepo.getReferenceById(nivelId));
                c.setEstado(activo);
                return c;
            });
            camara.setNombre(camDTO.getName());
            camara.setColor(camDTO.getColor());
            camara.setCoordenadas(codec.serializeCameraCoords(camDTO));
            camara.setAssignedSpots(codec.serializeJson(camDTO.getAssignedSpots()));
            camara.setNivel(nivelRepo.getReferenceById(nivelId));
            camara.setEstado(activo);
            camara.setTipo(TipoCamara.fromString(camDTO.getTipo()));
            if (camDTO.getParentSectionId() != null) {
                try {
                    Long parentSeccionId = resolveRef(camDTO.getParentSectionId(), sectionRefMap, "parentSectionId");
                    camara.setSeccion(seccionRepo.getReferenceById(parentSeccionId));
                } catch (IllegalArgumentException ignored) {
                    // Si no resuelve, queda null (camara global del nivel)
                }
            }
            camara = camaraRepo.save(camara);
            out.add(mapper.toCameraConfig(camara));
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  ARCHIVE
    // ═══════════════════════════════════════════════════════════════════

    @Transactional
    public void archivarParqueadero(Long parqueaderoId) {
        Parqueadero p = parqueaderoRepo.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        Estado archivado = estadoRepo.findByNombreIgnoreCase(ARCHIVADO)
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));

        for (Nivel nivel : nivelRepo.findByParqueaderoId(parqueaderoId)) {
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

    @Transactional
    public void deleteConfig(Long parqueaderoId) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo ADMIN o SUPER_ADMIN pueden borrar la configuracion");
        }
        Parqueadero p = parqueaderoRepo.findById(parqueaderoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parqueadero", parqueaderoId));
        if (p.getEmpresa() != null) {
            currentUser.requireEmpresa(p.getEmpresa().getId());
        }
        Estado archivado = estadoRepo.findByNombreIgnoreCase(ARCHIVADO)
                .orElseThrow(() -> new ResourceNotFoundException("Estado ARCHIVADO no encontrado"));
        for (Nivel nivel : nivelRepo.findByParqueaderoId(parqueaderoId)) {
            archivarNivelCascade(nivel, archivado);
        }
    }

    private void archivarNivelCascade(Nivel nivel, Estado archivado) {
        // Bulk via @Modifying @Query (1 UPDATE por tipo en vez de N)
        seccionRepo.archivarPorNivelId(nivel.getId(), archivado.getId());
        subSeccionRepo.archivarPorNivelId(nivel.getId(), archivado.getId());
        puntoParqueoRepo.archivarPorNivelId(nivel.getId(), archivado.getId());
        caminoRepo.archivarPorNivelId(nivel.getId(), archivado.getId());
        camaraRepo.archivarPorNivelId(nivel.getId(), archivado.getId());

        nivel.setEstado(archivado);
        nivelRepo.save(nivel);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Utilidades
    // ═══════════════════════════════════════════════════════════════════

    /** Intenta parsear el id como Long -> busca en BD. Si no, crea nuevo con el supplier. */
    private <T, R extends org.springframework.data.jpa.repository.JpaRepository<T, Long>> T resolveOrCreate(
            String idStr, R repo, java.util.function.Supplier<T> newEntitySupplier) {
        if (idStr != null) {
            try {
                Long dbId = Long.parseLong(idStr);
                return repo.findById(dbId).orElseGet(newEntitySupplier);
            } catch (NumberFormatException ignored) {
                // Es UUID del frontend -> nuevo
            }
        }
        return newEntitySupplier.get();
    }

    /** Resuelve una referencia: intenta como Long directo o busca en el refMap. */
    private Long resolveRef(String ref, Map<String, Long> refMap, String fieldName) {
        if (ref == null) throw new IllegalArgumentException(fieldName + " es requerido");
        if (refMap.containsKey(ref)) return refMap.get(ref);
        try {
            return Long.parseLong(ref);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + " = '" + ref + "' no se pudo resolver a un ID válido");
        }
    }

    private <T> Set<Long> collectDbIds(List<T> items, java.util.function.Function<T, String> idGetter) {
        Set<Long> out = new HashSet<>();
        if (items == null) return out;
        for (T it : items) {
            Long id = tryParseLong(idGetter.apply(it));
            if (id != null) out.add(id);
        }
        return out;
    }

    private Long tryParseLong(String s) {
        if (s == null) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
