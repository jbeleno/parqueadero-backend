package com.usco.parqueaderos_api.parking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usco.parqueaderos_api.parking.dto.config.CameraAssignedSpotDTO;
import com.usco.parqueaderos_api.parking.dto.config.CameraConfigDTO;
import com.usco.parqueaderos_api.parking.dto.config.CoordinateDTO;
import com.usco.parqueaderos_api.parking.dto.config.ParkingSpotCoordsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializacion/deserializacion JSON de los campos custom de la configuracion
 * del parqueadero (coordenadas, assignedSpots, etc.) que se persisten como
 * String en BD (columnas TEXT).
 *
 * Aislado de ParkingConfigService para mantener cohesion: codec puro, sin
 * dependencias del dominio mas alla de los DTOs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParkingConfigCodec {

    private final ObjectMapper objectMapper;

    public String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Error serializando: {}", e.getMessage());
            return null;
        }
    }

    public List<CoordinateDTO> deserializeCoordList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<CoordinateDTO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando coordenadas: {}", e.getMessage());
            return List.of();
        }
    }

    public ParkingSpotCoordsDTO deserializeSpotCoords(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ParkingSpotCoordsDTO.class);
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando coordenadas de punto: {}", e.getMessage());
            return null;
        }
    }

    /** Serializa nx/ny/nw/nh a JSON, omitiendo nulls. */
    public String serializeCameraCoords(CameraConfigDTO dto) {
        if (dto.getNx() == null && dto.getNy() == null
                && dto.getNw() == null && dto.getNh() == null) {
            return null;
        }
        Map<String, Double> map = new HashMap<>();
        if (dto.getNx() != null) map.put("nx", dto.getNx());
        if (dto.getNy() != null) map.put("ny", dto.getNy());
        if (dto.getNw() != null) map.put("nw", dto.getNw());
        if (dto.getNh() != null) map.put("nh", dto.getNh());
        return serializeJson(map);
    }

    public Map<String, Double> deserializeCameraCoords(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando coordenadas de camara: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Reader tolerante para assignedSpots. En BD puede haber:
     *   Legacy: ["117","118"]   -> [{spotId:"117", imageBox:null}, ...]
     *   Nuevo:  [{spotId, imageBox, imagePolygon}, ...]
     *
     * Detecta el formato leyendo como JsonNode y bifurca segun el tipo
     * del primer elemento.
     */
    public List<CameraAssignedSpotDTO> deserializeAssignedSpots(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray() || root.isEmpty()) return List.of();
            JsonNode first = root.get(0);
            if (first.isTextual()) {
                // Legacy
                List<CameraAssignedSpotDTO> out = new ArrayList<>();
                for (JsonNode n : root) {
                    out.add(new CameraAssignedSpotDTO(n.asText(), null));
                }
                return out;
            }
            return objectMapper.readValue(json,
                    new TypeReference<List<CameraAssignedSpotDTO>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando assignedSpots: {}", e.getMessage());
            return List.of();
        }
    }
}
