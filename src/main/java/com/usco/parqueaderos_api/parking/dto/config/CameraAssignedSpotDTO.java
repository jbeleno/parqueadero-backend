package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Asignacion de un punto de parqueo a una camara con su bounding box
 * dentro de la imagen. Las coordenadas del imageBox son normalizadas
 * (0..1) relativas a la imagen completa de la camara.
 *
 * El imageBox puede ser null si el admin asigno el spot pero aun no
 * dibujo la caja sobre la imagen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraAssignedSpotDTO {

    /** Id del punto de parqueo asignado (UUID nuevo o DB-id string). */
    private String spotId;

    /** Bounding box del spot dentro de la imagen. Null si aun no se dibujo. */
    private ImageBoxDTO imageBox;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageBoxDTO {
        private Double x;   // [0..1] esquina superior izquierda
        private Double y;   // [0..1]
        private Double w;   // [0..1] ancho
        private Double h;   // [0..1] alto
    }
}
