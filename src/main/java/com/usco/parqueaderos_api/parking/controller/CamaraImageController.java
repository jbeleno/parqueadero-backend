package com.usco.parqueaderos_api.parking.controller;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import com.usco.parqueaderos_api.parking.entity.Camara;
import com.usco.parqueaderos_api.parking.service.CamaraImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/camaras")
@RequiredArgsConstructor
public class CamaraImageController {

    private final CamaraImageService camaraImageService;

    /**
     * Sube/reemplaza la imagen actual de una camara.
     * Acepta JPG/PNG hasta 5MB. El backend resizea a max 1280x720.
     *
     * curl -X POST .../api/camaras/5/imagen \
     *      -H "Authorization: Bearer <token>" \
     *      -F "file=@frame.jpg"
     */
    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImagen(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Camara saved = camaraImageService.uploadImagen(id, file);
        Map<String, Object> data = Map.of(
                "cameraId", saved.getId().toString(),
                "imagenUrl", "/api/camaras/" + saved.getId() + "/imagen?t="
                        + (saved.getImagenTimestamp() != null
                            ? saved.getImagenTimestamp().toString() : ""),
                "imagenTimestamp", saved.getImagenTimestamp()
        );
        return ResponseEntity.ok(ApiResponse.ok(data, "Imagen actualizada"));
    }

    /**
     * Devuelve los bytes de la imagen como image/jpeg o image/png.
     * El parametro ?t se usa para cache-busting; el server lo ignora.
     */
    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> getImagen(@PathVariable Long id) {
        CamaraImageService.ImagenData img = camaraImageService.getImagen(id);
        return ResponseEntity.ok()
                .contentType(img.contentType() != null
                        ? MediaType.parseMediaType(img.contentType())
                        : MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate().getHeaderValue())
                .body(img.bytes());
    }
}
