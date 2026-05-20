package com.usco.parqueaderos_api.common.exception;

import com.usco.parqueaderos_api.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "ERR_NOT_FOUND"));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "ERR_DUPLICATE"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        // Codigos como ERR_POINT_OCCUPIED -> 409, ERR_INSUFFICIENT_FUNDS -> 422
        HttpStatus status = mapBusinessToStatus(ex.getErrorCode());
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "No tienes permiso para realizar esta accion",
                        "ERR_FORBIDDEN"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "ERR_BAD_CREDENTIALS"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "ERR_UNAUTHENTICATED"));
    }

    /**
     * Path/query param con tipo equivocado (ej. /api/camaras/abc/imagen cuando se esperaba Long).
     * El front suele caer aqui cuando manda su slug temporal ("camera-...") en vez del DB id.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valor valido";
        String msg = String.format(
                "El parametro '%s' recibio el valor '%s' pero se esperaba %s. "
                + "Si estas trabajando con cameras/secciones/etc, usa el id numerico de BD que devuelve "
                + "POST/PUT /api/parqueaderos/configuracion, no los slugs temporales del frontend.",
                ex.getName(), ex.getValue(), required);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg, "ERR_INVALID_PARAM"));
    }

    /** Body JSON ilegible (sintaxis invalida o tipos incompatibles). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "El body del request no se pudo leer. Verifica que sea JSON valido y que los tipos coincidan.",
                        "ERR_INVALID_BODY"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Error de validacion", "ERR_VALIDATION", fieldErrors));
    }

    /**
     * Catch-all. NO expone ex.getMessage() ni stack traces al cliente.
     * El detalle queda en logs internos para diagnostico.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Error inesperado en el servidor", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Error interno del servidor. Reporta este incidente al equipo tecnico.",
                        "ERR_INTERNAL"));
    }

    /** Mapea errorCodes a status HTTP semanticos. */
    private HttpStatus mapBusinessToStatus(String code) {
        if (code == null) return HttpStatus.UNPROCESSABLE_ENTITY;
        switch (code) {
            case "ERR_POINT_OCCUPIED":
            case "ERR_INVOICE_DUPLICATE":
            case "ERR_INVOICE_ALREADY_PAID":
                return HttpStatus.CONFLICT;
            case "ERR_MISSING_FIELDS":
            case "ERR_INVALID_AMOUNT":
            case "ERR_INVALID_STATE":
            case "ERR_INVALID_TRANSITION":
                return HttpStatus.BAD_REQUEST;
            default:
                return HttpStatus.UNPROCESSABLE_ENTITY;
        }
    }
}
