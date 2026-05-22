package com.usco.parqueaderos_api.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Cliente HTTP del sidecar OCR (Python + YOLO + PaddleOCR).
 *
 * El sidecar corre dentro del mismo deploy Dokploy, en la red interna,
 * y solo es alcanzable por nombre de servicio (no expuesto al exterior).
 *
 * Si OCR esta deshabilitado (app.ocr.enabled=false) o el sidecar no
 * responde, devuelve lista vacia y loggea: NO debe romper el upload de
 * imagen del flujo principal.
 */
@Component
@Slf4j
public class PlateReaderClient {

    private final RestClient client;
    private final boolean enabled;

    public PlateReaderClient(
            @Value("${app.ocr.url:http://ocr:8001}") String baseUrl,
            @Value("${app.ocr.enabled:true}") boolean enabled,
            @Value("${app.ocr.timeout-ms:10000}") int timeoutMs) {
        this.enabled = enabled;
        // Timeout generoso porque el OCR puede tardar ~2.4s por imagen
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        requestFactory.setReadTimeout(timeoutMs);
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        log.info("PlateReaderClient: enabled={}, baseUrl={}, timeoutMs={}", enabled, baseUrl, timeoutMs);
    }

    /**
     * Pide al sidecar que lea las placas en el frame.
     * Devuelve lista vacia si OCR esta deshabilitado, si el sidecar no responde,
     * o si no se detecto ninguna placa que pase los filtros.
     */
    public List<PlateReading> readPlates(Long camaraId, byte[] imageBytes, String contentType) {
        if (!enabled) return List.of();
        if (imageBytes == null || imageBytes.length == 0) return List.of();
        if (camaraId == null) return List.of();

        try {
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("camara_id", camaraId);
            String filename = "frame." + (contentType != null && contentType.contains("png") ? "png" : "jpg");
            form.add("file", new NamedByteArrayResource(imageBytes, filename));

            ReadPlateResponse resp = client.post()
                    .uri("/read-plate")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(ReadPlateResponse.class);

            if (resp == null || resp.placas() == null) return List.of();
            return resp.placas();
        } catch (ResourceAccessException e) {
            log.warn("OCR sidecar no responde (camara={}): {}", camaraId, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Error llamando OCR sidecar para camara {}: {}", camaraId, e.getMessage());
            return List.of();
        }
    }

    public record PlateReading(
            String placa,
            Double confianza,
            List<Integer> bbox
    ) {}

    private record ReadPlateResponse(
            List<PlateReading> placas,
            List<String> skipped_cooldown
    ) {}

    /** Spring necesita un Resource con filename para multipart; el ByteArrayResource default no lo tiene. */
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;
        NamedByteArrayResource(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename;
        }
        @Override
        public String getFilename() { return filename; }
    }
}
