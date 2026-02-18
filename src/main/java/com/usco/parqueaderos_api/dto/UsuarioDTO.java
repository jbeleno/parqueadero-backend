package com.usco.parqueaderos_api.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UsuarioDTO {
    private Long id;
    private String username;
    private String email;
    private Long personaId;
    private String personaNombre;
    private String personaDocumento;
    private Long estadoId;
    private String estadoNombre;
    private LocalDate fechaCreacion;
}
