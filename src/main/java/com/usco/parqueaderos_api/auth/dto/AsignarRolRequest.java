package com.usco.parqueaderos_api.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AsignarRolRequest {
    @NotNull(message = "El ID del rol es obligatorio")
    private Long rolId;
}
