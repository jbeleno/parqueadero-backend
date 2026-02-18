package com.usco.parqueaderos_api.parking.dto.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoordinateDTO {
    private double x;
    private double y;
}
