package com.usco.parqueaderos_api.parking.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Camara dentro de un piso. Coordenadas normalizadas (0..1)
 * relativas a la seccion padre.
 *
 * assignedSpots es lista de objetos {spotId, imageBox}. El backend
 * es tolerante al formato viejo (lista de strings) en lectura: si
 * detecta strings, los promueve a {spotId: string, imageBox: null}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CameraConfigDTO {
    private String id;
    private String name;
    private String parentSectionId;
    private Double nx;
    private Double ny;
    private Double nw;
    private Double nh;
    private List<CameraAssignedSpotDTO> assignedSpots;
    private String color;

    /** Rol funcional: "ENTRADA" | "SALIDA" | "SEGURIDAD". Default SEGURIDAD si no viene. */
    private String tipo;

    /** URL relativa para descargar la imagen. Null si la camara nunca recibio una. */
    private String imagenUrl;
    private LocalDateTime imagenTimestamp;
}
