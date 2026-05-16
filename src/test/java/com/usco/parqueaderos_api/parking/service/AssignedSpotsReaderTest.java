package com.usco.parqueaderos_api.parking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usco.parqueaderos_api.parking.dto.config.CameraAssignedSpotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica que el deserializer de assignedSpots de ParkingConfigService
 * sea tolerante a los dos formatos en BD:
 * - Legacy: ["117","118"]
 * - Nuevo:  [{"spotId":"117","imageBox":{"x":0.1,"y":0.2,"w":0.1,"h":0.1}}]
 *
 * Como el metodo es private uso reflection. El comportamiento es lo
 * suficientemente critico para tenerlo cubierto explicitamente.
 */
class AssignedSpotsReaderTest {

    private ParkingConfigService svc;
    private Method m;

    @BeforeEach
    void setUp() throws Exception {
        // Construyo una instancia con dependencias null - solo voy a invocar el metodo
        // que no las usa (deserializeAssignedSpots solo usa objectMapper).
        svc = new ParkingConfigService(
                null, null, null, null, null, null, null, null, null, null, null, null,
                new ObjectMapper(),
                null
        );
        m = ParkingConfigService.class.getDeclaredMethod("deserializeAssignedSpots", String.class);
        m.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    private List<CameraAssignedSpotDTO> invoke(String json) throws Exception {
        return (List<CameraAssignedSpotDTO>) m.invoke(svc, json);
    }

    @Test
    @DisplayName("null y blank devuelven lista vacia")
    void nullOrBlank_emptyList() throws Exception {
        assertTrue(invoke(null).isEmpty());
        assertTrue(invoke("").isEmpty());
        assertTrue(invoke("   ").isEmpty());
    }

    @Test
    @DisplayName("Legacy array de strings se promueve a objetos con imageBox=null")
    void legacy_arrayDeStrings() throws Exception {
        List<CameraAssignedSpotDTO> result = invoke("[\"117\",\"118\"]");
        assertEquals(2, result.size());
        assertEquals("117", result.get(0).getSpotId());
        assertNull(result.get(0).getImageBox());
        assertEquals("118", result.get(1).getSpotId());
        assertNull(result.get(1).getImageBox());
    }

    @Test
    @DisplayName("Nuevo formato con objetos se deserializa correcto")
    void nuevo_objetos() throws Exception {
        String json = "[{\"spotId\":\"117\",\"imageBox\":{\"x\":0.1,\"y\":0.2,\"w\":0.3,\"h\":0.4}}]";
        List<CameraAssignedSpotDTO> result = invoke(json);
        assertEquals(1, result.size());
        assertEquals("117", result.get(0).getSpotId());
        assertNotNull(result.get(0).getImageBox());
        assertEquals(0.1, result.get(0).getImageBox().getX());
        assertEquals(0.4, result.get(0).getImageBox().getH());
    }

    @Test
    @DisplayName("Nuevo formato con imageBox null en algun elemento se respeta")
    void nuevo_imageBoxNull() throws Exception {
        String json = "[{\"spotId\":\"117\",\"imageBox\":null},{\"spotId\":\"118\"}]";
        List<CameraAssignedSpotDTO> result = invoke(json);
        assertEquals(2, result.size());
        assertNull(result.get(0).getImageBox());
        assertNull(result.get(1).getImageBox());
    }

    @Test
    @DisplayName("Array vacio devuelve lista vacia")
    void arrayVacio() throws Exception {
        assertTrue(invoke("[]").isEmpty());
    }

    @Test
    @DisplayName("JSON invalido devuelve lista vacia y no lanza")
    void jsonInvalido_emptyList() throws Exception {
        assertTrue(invoke("not-a-json").isEmpty());
    }
}
