package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Subsección dentro de una sección — mapa del canvas del frontend */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubsectionConfigDTO {
    private String id;                // UUID (create) o DB-id string (update/get)
    private String name;
    private String description;
    private String acronym;
    private String parentSectionId;   // referencia al id de la sección padre
    private List<CoordinateDTO> coordinates;
    private Integer parkingSpots;     // cantidad de puntos que caben
}
