package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Punto de parqueo individual con bounding-box del canvas */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpotConfigDTO {
    private String id;               // UUID (create) o DB-id string (update/get)
    private String subsectionId;     // referencia al id de la subsección padre
    private ParkingSpotCoordsDTO coordinates;
    private String acronym;
    private String description;
    private String type;             // nombre del tipo_punto_parqueo (e.g. "administrativo")
}
