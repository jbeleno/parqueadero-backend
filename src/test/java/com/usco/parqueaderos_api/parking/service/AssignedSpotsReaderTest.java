package com.usco.parqueaderos_api.parking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usco.parqueaderos_api.parking.dto.config.CameraAssignedSpotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que el deserializer de assignedSpots de ParkingConfigCodec
 * sea tolerante a los formatos en BD:
 * - Legacy: ["117","118"]
 * - Nuevo:  [{"spotId":"117","imageBox":{"x":0.1,...}, "imagePolygon":[[...]]}]
 */
class AssignedSpotsReaderTest {

    private ParkingConfigCodec codec;

    @BeforeEach
    void setUp() {
        codec = new ParkingConfigCodec(new ObjectMapper());
    }

    @Test
    @DisplayName("null y blank devuelven lista vacia")
    void nullOrBlank_emptyList() {
        assertTrue(codec.deserializeAssignedSpots(null).isEmpty());
        assertTrue(codec.deserializeAssignedSpots("").isEmpty());
        assertTrue(codec.deserializeAssignedSpots("   ").isEmpty());
    }

    @Test
    @DisplayName("Legacy array de strings se promueve a objetos con imageBox=null")
    void legacy_arrayDeStrings() {
        List<CameraAssignedSpotDTO> result = codec.deserializeAssignedSpots("[\"117\",\"118\"]");
        assertEquals(2, result.size());
        assertEquals("117", result.get(0).getSpotId());
        assertNull(result.get(0).getImageBox());
        assertEquals("118", result.get(1).getSpotId());
    }

    @Test
    @DisplayName("Nuevo formato con objetos se deserializa correcto")
    void nuevo_objetos() {
        String json = "[{\"spotId\":\"117\",\"imageBox\":{\"x\":0.1,\"y\":0.2,\"w\":0.3,\"h\":0.4}}]";
        List<CameraAssignedSpotDTO> result = codec.deserializeAssignedSpots(json);
        assertEquals(1, result.size());
        assertEquals("117", result.get(0).getSpotId());
        assertNotNull(result.get(0).getImageBox());
        assertEquals(0.1, result.get(0).getImageBox().getX());
        assertEquals(0.4, result.get(0).getImageBox().getH());
    }

    @Test
    @DisplayName("Nuevo formato con imagePolygon")
    void nuevo_imagePolygon() {
        String json = "[{\"spotId\":\"117\",\"imagePolygon\":[[0.1,0.2],[0.3,0.2],[0.3,0.4],[0.1,0.4]]}]";
        List<CameraAssignedSpotDTO> result = codec.deserializeAssignedSpots(json);
        assertEquals(1, result.size());
        assertEquals("117", result.get(0).getSpotId());
        assertNotNull(result.get(0).getImagePolygon());
        assertEquals(4, result.get(0).getImagePolygon().size());
    }

    @Test
    @DisplayName("Nuevo formato con imageBox null en algun elemento se respeta")
    void nuevo_imageBoxNull() {
        String json = "[{\"spotId\":\"117\",\"imageBox\":null},{\"spotId\":\"118\"}]";
        List<CameraAssignedSpotDTO> result = codec.deserializeAssignedSpots(json);
        assertEquals(2, result.size());
        assertNull(result.get(0).getImageBox());
        assertNull(result.get(1).getImageBox());
    }

    @Test
    @DisplayName("Array vacio devuelve lista vacia")
    void arrayVacio() {
        assertTrue(codec.deserializeAssignedSpots("[]").isEmpty());
    }

    @Test
    @DisplayName("JSON invalido devuelve lista vacia y no lanza")
    void jsonInvalido_emptyList() {
        assertTrue(codec.deserializeAssignedSpots("not-a-json").isEmpty());
    }
}
