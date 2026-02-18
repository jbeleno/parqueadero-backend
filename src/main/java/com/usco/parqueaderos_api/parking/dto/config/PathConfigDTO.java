package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Camino / ruta dentro de un piso (polyline en el canvas) */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathConfigDTO {
    private String id;               // UUID (create) o DB-id string (update/get)
    private String type;             // "polyline", etc.
    private List<CoordinateDTO> coordinates;
}
