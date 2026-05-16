package com.usco.parqueaderos_api.common.storage;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Implementacion default: guarda las imagenes en el filesystem local.
 *
 * En produccion (Dokploy) el directorio app.images.dir debe apuntar a un
 * volumen Docker persistente — sino las imagenes se pierden con cada
 * Deploy.
 *
 * Estructura en disco:
 *   {app.images.dir}/{key}.{ext}
 *
 * Donde key = "camaras/{cameraId}" o cualquier slug logico.
 */
@Service
@Slf4j
public class FileSystemImageStorageService implements ImageStorageService {

    @Value("${app.images.dir:/var/parqueaderos/images}")
    private String rootDir;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(Paths.get(rootDir));
            log.info("ImageStorage filesystem inicializado en: {}", rootDir);
        } catch (IOException e) {
            log.error("No se pudo crear el directorio de imagenes en {}: {}", rootDir, e.getMessage());
        }
    }

    @Override
    public String save(String key, byte[] bytes, String contentType) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key obligatoria");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes vacios");
        }
        String ext = extensionFor(contentType);
        Path target = Paths.get(rootDir, sanitize(key) + "." + ext);
        try {
            Files.createDirectories(target.getParent());
            // tmp + rename atomico
            Path tmp = Paths.get(target + ".tmp");
            Files.write(tmp, bytes);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.debug("Imagen guardada: {} ({} bytes)", target, bytes.length);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Fallo guardando imagen para key " + key, e);
        }
    }

    @Override
    public byte[] read(String key) {
        Path target = resolveExisting(key);
        if (target == null) {
            throw new ImageNotFoundException("No existe imagen para key: " + key);
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new RuntimeException("Fallo leyendo imagen " + target, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            for (String ext : new String[]{"jpg", "jpeg", "png"}) {
                Path p = Paths.get(rootDir, sanitize(key) + "." + ext);
                Files.deleteIfExists(p);
            }
        } catch (IOException e) {
            log.warn("Fallo borrando imagen key {}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        return resolveExisting(key) != null;
    }

    /** Resuelve la primera extension valida en disco para una key. */
    private Path resolveExisting(String key) {
        for (String ext : new String[]{"jpg", "jpeg", "png"}) {
            Path p = Paths.get(rootDir, sanitize(key) + "." + ext);
            if (Files.exists(p)) return p;
        }
        return null;
    }

    private String extensionFor(String contentType) {
        if (contentType == null) return "jpg";
        switch (contentType.toLowerCase()) {
            case "image/png":  return "png";
            case "image/jpeg":
            case "image/jpg":  return "jpg";
            default:           return "jpg";
        }
    }

    /** Sanitiza la key: solo letras, numeros, slash, guion y underscore. */
    private String sanitize(String key) {
        return key.replaceAll("[^a-zA-Z0-9/_-]", "_");
    }
}
