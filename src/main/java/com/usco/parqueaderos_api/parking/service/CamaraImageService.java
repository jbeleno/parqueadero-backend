package com.usco.parqueaderos_api.parking.service;

import com.usco.parqueaderos_api.auth.service.CurrentUserService;
import com.usco.parqueaderos_api.common.event.CamaraImagenActualizadaEvent;
import com.usco.parqueaderos_api.common.exception.BusinessException;
import com.usco.parqueaderos_api.common.exception.ResourceNotFoundException;
import com.usco.parqueaderos_api.common.storage.ImageStorageService;
import com.usco.parqueaderos_api.parking.entity.Camara;
import com.usco.parqueaderos_api.parking.repository.CamaraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Gestiona el upload/lectura de la imagen de una camara con:
 * - Validaciones (mime type real, tamano)
 * - Resize automatico a max 1280x720
 * - RBAC (ADMIN/SUPER_ADMIN para upload)
 * - Multi-tenant (camara debe ser de la empresa del operador)
 * - Storage agnostico via ImageStorageService
 * - Evento WebSocket al actualizar
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CamaraImageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5MB
    private static final int  MAX_WIDTH  = 1280;
    private static final int  MAX_HEIGHT = 720;
    private static final Set<String> MIME_PERMITIDOS = Set.of("image/jpeg", "image/jpg", "image/png");

    private final CamaraRepository camaraRepo;
    private final ImageStorageService imageStorage;
    private final CurrentUserService currentUser;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Camara uploadImagen(Long camaraId, MultipartFile file) {
        if (!currentUser.isAdmin() && !currentUser.isSuperAdmin()) {
            throw new AccessDeniedException("Solo ADMIN o SUPER_ADMIN pueden subir imagenes de camara");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Archivo vacio", "ERR_FILE_EMPTY");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(
                    "El archivo excede el maximo de 5MB",
                    "ERR_FILE_TOO_LARGE");
        }

        Camara camara = camaraRepo.findById(camaraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camara", camaraId));

        // Multi-tenant: la camara debe pertenecer a un parqueadero de la empresa del operador
        Long empresaId = camara.getNivel() != null
                && camara.getNivel().getParqueadero() != null
                && camara.getNivel().getParqueadero().getEmpresa() != null
                ? camara.getNivel().getParqueadero().getEmpresa().getId() : null;
        if (empresaId != null) {
            currentUser.requireEmpresa(empresaId);
        }

        byte[] rawBytes;
        try {
            rawBytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Fallo leyendo el archivo", "ERR_FILE_READ");
        }

        // Validar mime real (NO confiar en file.getContentType — viene del cliente)
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (image == null) {
                throw new BusinessException(
                        "El archivo no es una imagen valida (JPG/PNG)",
                        "ERR_INVALID_IMAGE");
            }
        } catch (IOException e) {
            throw new BusinessException(
                    "Fallo decodificando la imagen",
                    "ERR_INVALID_IMAGE");
        }

        String mimeReal = inferirMime(file.getContentType(), file.getOriginalFilename());
        if (!MIME_PERMITIDOS.contains(mimeReal)) {
            throw new BusinessException(
                    "Tipo de imagen no permitido. Acepta JPG/PNG.",
                    "ERR_INVALID_MIME");
        }

        // Resize a max 1280x720 conservando aspect ratio
        BufferedImage redimensionada = resize(image, MAX_WIDTH, MAX_HEIGHT);

        // Re-encode al mismo formato
        String formato = mimeReal.equals("image/png") ? "png" : "jpg";
        byte[] bytesFinales;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(redimensionada, formato, baos);
            bytesFinales = baos.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Fallo encodeando la imagen", "ERR_IMAGE_ENCODE");
        }

        String key = "camaras/" + camara.getId();
        String path = imageStorage.save(key, bytesFinales, "image/" + formato);

        LocalDateTime ahora = LocalDateTime.now();
        camara.setImagenPath(path);
        camara.setImagenTimestamp(ahora);
        camara.setImagenContentType("image/" + formato);
        Camara saved = camaraRepo.save(camara);

        // Evento WebSocket
        Long parqueaderoId = saved.getNivel() != null && saved.getNivel().getParqueadero() != null
                ? saved.getNivel().getParqueadero().getId() : null;
        eventPublisher.publishEvent(new CamaraImagenActualizadaEvent(
                this, saved.getId(), parqueaderoId, ahora));

        log.info("Imagen actualizada para camara {} ({} bytes, {})",
                saved.getId(), bytesFinales.length, formato);
        return saved;
    }

    @Transactional(readOnly = true)
    public ImagenData getImagen(Long camaraId) {
        Camara camara = camaraRepo.findById(camaraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camara", camaraId));

        // Multi-tenant en lectura: solo SUPER_ADMIN ve cualquier camara;
        // ADMIN y USER solo de su empresa (PII / data sensible en la imagen).
        Long empresaId = camara.getNivel() != null
                && camara.getNivel().getParqueadero() != null
                && camara.getNivel().getParqueadero().getEmpresa() != null
                ? camara.getNivel().getParqueadero().getEmpresa().getId() : null;
        if (empresaId != null) {
            currentUser.requireEmpresa(empresaId);
        }

        if (camara.getImagenPath() == null) {
            throw new ResourceNotFoundException("Imagen de camara", camaraId);
        }
        String key = "camaras/" + camara.getId();
        byte[] bytes = imageStorage.read(key);
        return new ImagenData(bytes, camara.getImagenContentType(), camara.getImagenTimestamp());
    }

    /** Resize manteniendo aspect ratio. Solo achica, nunca agranda. */
    private BufferedImage resize(BufferedImage src, int maxWidth, int maxHeight) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxWidth && h <= maxHeight) return src;

        double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
        int newW = (int) Math.round(w * scale);
        int newH = (int) Math.round(h * scale);

        BufferedImage out = new BufferedImage(newW, newH,
                src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return out;
    }

    /** Decide el mime real basandose en la extension del filename (fallback al header). */
    private String inferirMime(String headerContentType, String filename) {
        if (filename != null) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        }
        if (headerContentType == null) return "image/jpeg";
        return headerContentType.toLowerCase();
    }

    /** Tupla de respuesta para el controller. */
    public record ImagenData(byte[] bytes, String contentType, LocalDateTime timestamp) {}
}
