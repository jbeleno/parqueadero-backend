package com.usco.parqueaderos_api.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " con id " + id + " no encontrado");
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
