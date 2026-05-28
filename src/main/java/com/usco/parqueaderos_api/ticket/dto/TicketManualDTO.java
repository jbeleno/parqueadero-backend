package com.usco.parqueaderos_api.ticket.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Crear ticket manualmente, sin OCR. Cubre 3 escenarios reales en parqueaderos:
 *
 * 1) **Carro de empresa registrado** (cliente conocido): operario digita la placa.
 *    Si la placa ya esta en BD, se reusa el Vehiculo existente (incluida la
 *    persona asociada). Caso comun cuando la camara fallo.
 *
 * 2) **Carro empresa que entra POR PRIMERA VEZ**: operario digita placa + selecciona
 *    persona ya registrada (`personaId`) + tipoVehiculo. Se crea Vehiculo NO visitante.
 *    Caso de un empleado/cliente nuevo de la empresa.
 *
 * 3) **Visitante (placa ilegible o desconocida)**: si `placa` es null/vacia se
 *    autogenera "VIS-YYYYMMDD-HHMMSS-XXXX" con esVisitante=true. Si la placa se
 *    digita pero no existe en BD y NO viene personaId, se crea como esVisitante=true.
 *
 * Ya sea creando o reusando, atomico en una sola transaccion: crea Vehiculo (si
 * no existe) y luego llama a save() del ticket reutilizando toda la validacion.
 */
@Data
public class TicketManualDTO {

    @NotNull(message = "El parqueadero es obligatorio")
    private Long parqueaderoId;

    @NotNull(message = "El punto de parqueo es obligatorio")
    private Long puntoParqueoId;

    @NotNull(message = "La tarifa es obligatoria")
    private Long tarifaId;

    @NotNull(message = "El tipo de vehiculo es obligatorio")
    private Long tipoVehiculoId;

    /**
     * Placa digitada por el operario. Si null/vacia se autogenera "VIS-..." y se
     * marca esVisitante=true (caso "vehiculo sin placa legible").
     */
    @Size(max = 20, message = "placa max 20 caracteres")
    private String placa;

    /** Color del vehiculo (opcional, ayuda al cierre). */
    @Size(max = 50, message = "color max 50 caracteres")
    private String color;

    /**
     * Persona registrada en BD asociada al vehiculo (opcional). Si viene:
     * - El vehiculo creado NO se marca como visitante (carro conocido de empresa).
     * - Util para registrar empleados/clientes que entran por primera vez aun
     *   sin OCR.
     * Si null: el vehiculo se crea como visitante (esVisitante=true).
     */
    private Long personaId;

    /** Observacion del operario para auditoria (ej: "OCR no lectura, vidrio sucio"). */
    @Size(max = 500, message = "observacion max 500 caracteres")
    private String observacion;
}
