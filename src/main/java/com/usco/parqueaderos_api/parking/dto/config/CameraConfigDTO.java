package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Camara dentro de un piso. Coordenadas normalizadas (0..1)
 * relativas a la seccion padre.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraConfigDTO {
    private String id;                  // UUID (create) o DB-id string (update/get)
    private String name;
    private String parentSectionId;     // ref a seccion (UUID nuevo o DB-id existente)
    private Double nx;                  // coordenada x normalizada [0..1]
    private Double ny;                  // coordenada y normalizada [0..1]
    private Double nw;                  // ancho normalizado [0..1]
    private Double nh;                  // alto normalizado [0..1]
    private List<String> assignedSpots; // ids de puntos asignados (refs)
    private String color;
}
