package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Sección dentro de un piso — mapa del canvas del frontend */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionConfigDTO {
    private String id;          // UUID (create) o DB-id string (update/get)
    private String name;
    private String description;
    private String acronym;
    private List<CoordinateDTO> coordinates;
}
