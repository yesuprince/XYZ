package com.xyz.booking.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xyz.booking.dto.ErrorResponse;
import com.xyz.booking.dto.ExternalErrorResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.TimeoutException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidBookingException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBooking(InvalidBookingException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(ExternalApiRequestException.class)
    public ResponseEntity<ErrorResponse> handleExternalApi(ExternalApiRequestException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("EXTERNAL_API_ERROR", ex.getMessage()));
    }
    
    @ExceptionHandler(ExternalApiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailable(ExternalApiUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponse("SERVICE_UNAVAILABLE", ex.getMessage()));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(BookingNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("404", "Booking not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeign(FeignException ex) {
        int status = ex.status();
        String body = ex.contentUTF8();
        String errorMessage = "";
        log.error("Feign call failed: status={} body={}", status, body);
        try {
            ExternalErrorResponse errorResponse = new ObjectMapper().readValue(body, ExternalErrorResponse.class);
            errorMessage = errorResponse.error();
        } catch (Exception ignored) {

        }

        return ResponseEntity.status(status).body(new ErrorResponse("EXTERNAL_API_ERROR", errorMessage));
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeout(TimeoutException ex) {
        return ResponseEntity.status(504).body(new ErrorResponse("EXTERNAL_API_TIMEOUT", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError().body(new ErrorResponse("INTERNAL_ERROR", ex.getMessage()));
    }
}
