package com.usco.parqueaderos_api.parking.entity;

/**
 * Rol funcional de la camara en el parqueadero:
 * - ENTRADA: vigila el acceso de entrada al parqueadero (lectura de placa = registro de ingreso)
 * - SALIDA:  vigila el acceso de salida (lectura de placa = registro de egreso)
 * - SEGURIDAD: cobertura general, no dispara entradas/salidas pero sigue leyendo placas
 *
 * El backend solo emite el evento PLACA_DETECTADA con el tipo. El frontend
 * decide que hacer (registrar ticket, alertar, etc.).
 */
public enum TipoCamara {
    ENTRADA,
    SALIDA,
    SEGURIDAD
}
