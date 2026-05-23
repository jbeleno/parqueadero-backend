package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.parking.dto.config.*;
import com.usco.parqueaderos_api.parking.entity.*;
import com.usco.parqueaderos_api.parking.repository.NivelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Convierte entidades JPA a DTOs de configuracion. Sin logica de persistencia
 * ni reglas de negocio, solo mapeo. Usa ParkingConfigCodec para los campos
 * que viven como JSON en BD.
 */
@Component
@RequiredArgsConstructor
public class ParkingConfigMapper {

    private static final String ARCHIVADO = "ARCHIVADO";

    private final ParkingConfigCodec codec;
    private final NivelRepository nivelRepo;

    public ParkingLotInfoDTO toParkingLotInfo(Parqueadero p) {
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
        if (p.getHoraInicio() != null) {
            dto.setHoraInicio(p.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (p.getHoraFin() != null) {
            dto.setHoraFin(p.getHoraFin().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (p.getEmpresa() != null) {
            dto.setEmpresaId(p.getEmpresa().getId());
            dto.setEmpresaNombre(p.getEmpresa().getNombre());
        }
        if (p.getCiudad() != null) {
            dto.setCiudadId(p.getCiudad().getId());
            dto.setCiudadNombre(p.getCiudad().getNombre());
        }
        if (p.getTipoParqueadero() != null) {
            dto.setTipoParqueaderoId(p.getTipoParqueadero().getId());
            dto.setTipoParqueaderoNombre(p.getTipoParqueadero().getNombre());
        }
        if (p.getEstado() != null) {
            dto.setEstadoId(p.getEstado().getId());
            dto.setEstadoNombre(p.getEstado().getNombre());
        }
        dto.setNumeroPisos(
                nivelRepo.findByParqueaderoIdAndEstadoNombreNot(p.getId(), ARCHIVADO).size()
        );
        return dto;
    }

    public SectionConfigDTO toSectionConfig(Seccion s) {
        SectionConfigDTO dto = new SectionConfigDTO();
        dto.setId(s.getId().toString());
        dto.setName(s.getNombre());
        dto.setDescription(s.getDescripcion());
        dto.setAcronym(s.getAcronimo());
        dto.setCoordinates(codec.deserializeCoordList(s.getCoordenadas()));
        return dto;
    }

    public SubsectionConfigDTO toSubsectionConfig(SubSeccion ss) {
        SubsectionConfigDTO dto = new SubsectionConfigDTO();
        dto.setId(ss.getId().toString());
        dto.setName(ss.getNombre());
        dto.setDescription(ss.getDescripcion());
        dto.setAcronym(ss.getAcronimo());
        dto.setParentSectionId(ss.getSeccion() != null ? ss.getSeccion().getId().toString() : null);
        dto.setCoordinates(codec.deserializeCoordList(ss.getCoordenadas()));
        dto.setParkingSpots(ss.getCantidadPuntos());
        return dto;
    }

    public ParkingSpotConfigDTO toParkingSpotConfig(PuntoParqueo pp) {
        ParkingSpotConfigDTO dto = new ParkingSpotConfigDTO();
        dto.setId(pp.getId().toString());
        dto.setSubsectionId(pp.getSubSeccion() != null ? pp.getSubSeccion().getId().toString() : null);
        dto.setAcronym(pp.getAcronimo());
        dto.setDescription(pp.getDescripcion());
        dto.setCoordinates(codec.deserializeSpotCoords(pp.getCoordenadas()));
        if (pp.getTipoPuntoParqueo() != null) dto.setType(pp.getTipoPuntoParqueo().getNombre());
        return dto;
    }

    public PathConfigDTO toPathConfig(Camino c) {
        PathConfigDTO dto = new PathConfigDTO();
        dto.setId(c.getId().toString());
        dto.setType(c.getTipo());
        dto.setCoordinates(codec.deserializeCoordList(c.getCoordenadas()));
        return dto;
    }

    public CameraConfigDTO toCameraConfig(Camara c) {
        CameraConfigDTO dto = new CameraConfigDTO();
        dto.setId(c.getId().toString());
        dto.setName(c.getNombre());
        dto.setColor(c.getColor());
        dto.setTipo(c.getTipo() != null ? c.getTipo().name() : null);
        if (c.getSeccion() != null) {
            dto.setParentSectionId(c.getSeccion().getId().toString());
        }
        Map<String, Double> coords = codec.deserializeCameraCoords(c.getCoordenadas());
        if (coords != null) {
            dto.setNx(coords.get("nx"));
            dto.setNy(coords.get("ny"));
            dto.setNw(coords.get("nw"));
            dto.setNh(coords.get("nh"));
        }
        dto.setAssignedSpots(codec.deserializeAssignedSpots(c.getAssignedSpots()));
        if (c.getImagenPath() != null && c.getImagenTimestamp() != null) {
            dto.setImagenUrl("/api/camaras/" + c.getId() + "/imagen?t=" + c.getImagenTimestamp());
            dto.setImagenTimestamp(c.getImagenTimestamp());
        }
        return dto;
    }
}
