package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Asignacion de un punto de parqueo a una camara con su bounding box
 * y/o poligono dentro de la imagen. Las coordenadas son normalizadas
 * (0..1) relativas a la imagen completa de la camara.
 *
 * imageBox y imagePolygon pueden ser null si el admin asigno el spot
 * pero aun no dibujo nada sobre la imagen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraAssignedSpotDTO {

    /** Id del punto de parqueo asignado (UUID nuevo o DB-id string). */
    private String spotId;

    /** Bounding box del spot dentro de la imagen. Null si no se dibujo o si se uso poligono. */
    private ImageBoxDTO imageBox;

    /**
     * Poligono (normalmente 4 esquinas) para representar la perspectiva
     * real del spot — un cajon visto en angulo se ve como trapecio, no
     * como rectangulo. Formato: [[x,y], [x,y], ...] con coordenadas
     * normalizadas 0..1. Null si solo se uso imageBox.
     */
    private List<List<Double>> imagePolygon;

    /** Constructor legacy usado por el reader tolerante (formato viejo: array de strings). */
    public CameraAssignedSpotDTO(String spotId, ImageBoxDTO imageBox) {
        this.spotId = spotId;
        this.imageBox = imageBox;
        this.imagePolygon = null;
    }

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
