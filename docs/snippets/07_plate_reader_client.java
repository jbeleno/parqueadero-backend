// PlateReaderClient.java — degradación silenciosa del sidecar OCR (Figura 19)
public List<PlateReading> readPlates(Long camaraId, byte[] imageBytes,
                                     String contentType) {
    if (!enabled) return List.of();
    if (imageBytes == null || imageBytes.length == 0) return List.of();
    if (camaraId == null) return List.of();

    try {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("camara_id", camaraId);
        String filename = "frame." + (contentType != null
                && contentType.contains("png") ? "png" : "jpg");
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
        // Sidecar caído o timeout → log y devuelve lista vacía
        log.warn("OCR sidecar no responde (camara={}): {}",
                camaraId, e.getMessage());
        return List.of();
    } catch (Exception e) {
        // Cualquier otro error → log y degradación silenciosa
        log.error("Error llamando OCR sidecar para camara {}: {}",
                camaraId, e.getMessage());
        return List.of();
    }
}
