package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO raíz: contiene toda la configuración/diseño de un parqueadero.
 * El frontend lo envía al guardar y lo recibe al dibujar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotConfigDTO {
    private ParkingLotInfoDTO parkingLot;
    private List<FloorConfigDTO> floors;
}
