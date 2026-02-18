package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Un piso (nivel) con todas sus entidades dibujables */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FloorConfigDTO {
    private String id;               // UUID (create) o DB-id string (update/get)
    private String name;
    private List<SectionConfigDTO> sections;
    private List<SubsectionConfigDTO> subsections;
    private List<ParkingSpotConfigDTO> parkingSpots;
    private List<PathConfigDTO> paths;
}
