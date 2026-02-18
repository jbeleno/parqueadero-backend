package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpotCoordsDTO {
    private CoordinateDTO topLeft;
    private CoordinateDTO topRight;
    private CoordinateDTO bottomLeft;
    private CoordinateDTO bottomRight;
}
