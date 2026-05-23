package com.usco.parqueaderos_api.ocr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests minimos del cliente HTTP al sidecar OCR.
 * Solo verifica el contrato de fallo seguro (degraded mode): si el sidecar
 * esta apagado, no responde, o la llamada es invalida, retorna lista vacia
 * en lugar de propagar excepcion al listener async.
 *
 * Para integracion real con el sidecar ver test de integracion (deploy + curl).
 */
class PlateReaderClientTest {

    @Test
    void disabled_returns_empty_without_calling_http() {
        PlateReaderClient client = new PlateReaderClient(
                "http://localhost:9999",  // url invalida
                false,                     // enabled=false
                100
        );

        List<PlateReaderClient.PlateReading> placas = client.readPlates(1L, new byte[]{1, 2, 3}, "image/jpeg");

        assertNotNull(placas);
        assertTrue(placas.isEmpty(), "Con OCR deshabilitado debe retornar lista vacia sin llamar HTTP");
    }

    @Test
    void empty_bytes_returns_empty() {
        PlateReaderClient client = new PlateReaderClient("http://localhost:9999", true, 100);
        assertTrue(client.readPlates(1L, new byte[]{}, "image/jpeg").isEmpty());
        assertTrue(client.readPlates(1L, null, "image/jpeg").isEmpty());
    }

    @Test
    void null_camara_id_returns_empty() {
        PlateReaderClient client = new PlateReaderClient("http://localhost:9999", true, 100);
        assertTrue(client.readPlates(null, new byte[]{1}, "image/jpeg").isEmpty());
    }

    @Test
    void sidecar_unreachable_returns_empty_does_not_throw() {
        // Sidecar no existe en este puerto: debe degradar a lista vacia, no propagar
        PlateReaderClient client = new PlateReaderClient(
                "http://127.0.0.1:1",  // puerto reservado, siempre conn refused
                true,
                500                    // timeout corto
        );

        List<PlateReaderClient.PlateReading> placas = client.readPlates(1L, new byte[]{1, 2, 3}, "image/jpeg");

        assertNotNull(placas);
        assertTrue(placas.isEmpty(), "Sidecar caido NO debe propagar excepcion al listener async");
    }
}
