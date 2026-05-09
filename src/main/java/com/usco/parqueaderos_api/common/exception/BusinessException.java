package com.usco.parqueaderos_api.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String message) {
        this(message, "ERR_BUSINESS");
    }

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
