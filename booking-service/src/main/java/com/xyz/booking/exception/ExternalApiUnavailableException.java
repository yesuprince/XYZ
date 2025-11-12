package com.xyz.booking.exception;

public class ExternalApiUnavailableException extends RuntimeException {
    public ExternalApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalApiUnavailableException(String message) {
        super(message);
    }
}
