package com.xyz.booking.exception;

public class ExternalApiRequestException extends RuntimeException {
    public ExternalApiRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalApiRequestException(String message) {
        super(message);
    }
}
