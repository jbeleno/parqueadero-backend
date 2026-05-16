package com.usco.parqueaderos_api.common.storage;

/**
 * Abstraccion de almacenamiento de imagenes. Permite migrar de filesystem
 * a S3/R2/MinIO sin tocar controllers ni services de dominio.
 *
 * Implementaciones disponibles:
 * - FileSystemImageStorageService (MVP, default)
 * - (futuro) S3ImageStorageService
 */
public interface ImageStorageService {

    /**
     * Guarda los bytes de una imagen identificada por una key logica.
     * Si ya existia una imagen con esa key, la reemplaza (no historial).
     *
     * @param key clave logica (ej. "camaras/5")
     * @param bytes contenido binario ya validado/resizeado
     * @param contentType "image/jpeg" o "image/png"
     * @return identificador opaco del recurso almacenado (path relativo, URL S3, etc.)
     */
    String save(String key, byte[] bytes, String contentType);

    /**
     * Lee los bytes asociados a una key.
     * @throws ImageNotFoundException si no existe
     */
    byte[] read(String key);

    /** Borra el recurso. Idempotente (no falla si no existe). */
    void delete(String key);

    /** Si existe contenido bajo esa key. */
    boolean exists(String key);

    /** Excepcion comun cuando una key no tiene contenido asociado. */
    class ImageNotFoundException extends RuntimeException {
        public ImageNotFoundException(String message) { super(message); }
    }
}
