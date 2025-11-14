package com.xyz.booking.validation;

import com.xyz.booking.clients.LicenseClient;
import com.xyz.booking.clients.PricingClient;
import com.xyz.booking.dto.*;
import com.xyz.booking.exception.ExternalApiRequestException;
import com.xyz.booking.exception.ExternalApiUnavailableException;
import com.xyz.booking.exception.InvalidBookingException;
import com.xyz.booking.properties.ValidationProperties;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
@RequiredArgsConstructor
public class Validator {
    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ScheduledExecutorService retryScheduler;
    private final LicenseClient licenseClient;
    private final PricingClient pricingClient;
    private final ValidationProperties properties;

    @Value("${external.apis.timeout}")
    private long externalApiTimeout;
    private Retry externalApiRetry;
    private CircuitBreaker externalApiCircuit;

    @PostConstruct
    public void init() {
        this.externalApiRetry = retryRegistry.retry("externalApiRetry");
        this.externalApiCircuit = circuitBreakerRegistry.circuitBreaker("externalApiCircuit");
    }

    public CompletableFuture<Void> validateReservationDuration(BookingRequest bookingRequest) {
        return CompletableFuture.runAsync(() -> {
            long days = ChronoUnit.DAYS.between(
                    bookingRequest.reservationStartDate(),
                    bookingRequest.reservationEndDate()
            );

            if (days <= 0) {
                throw new InvalidBookingException("Reservation end date must be after start date.");
            }
            if (days > properties.getMaxDays()) {
                throw new InvalidBookingException("Reservation exceeds " + properties.getMaxDays() + " days.");
            }
        });
    }


    public CompletableFuture<LicenseResponse> validateLicense(BookingRequest request) {
        String licenseNo = request.drivingLicenseNumber();
        log.info("Validating license [{}]", licenseNo);

        // API call
        Supplier<CompletableFuture<LicenseResponse>> licenseApiCall =
                () -> asyncApiCall(() -> licenseClient.getLicenseDetails(
                        new LicenseRequest(licenseNo)
                ));

        // Apply Retry + CircuitBreaker
        CompletableFuture<LicenseResponse> future = applyResilience(
                licenseApiCall,
                () -> "License Service temporarily unavailable",
                () -> licenseNo,
                LicenseResponse.class
        );

        // Business validation
        return future.thenApply(licenseResponse -> {
            validateLicenseBusinessRules(licenseResponse, licenseNo);
            log.info("License [{}] validated successfully", licenseNo);
            return licenseResponse;
        });
    }

    private void validateLicenseBusinessRules(LicenseResponse response, String licenseNo) {
        if (response == null) {
            throw new ExternalApiRequestException("License API returned empty response");
        }

        if (response.expiryDate().isBefore(LocalDate.now())) {
            throw new InvalidBookingException("Driving License is expired");
        }

        long licenseAge = ChronoUnit.YEARS.between(response.issueDate(), LocalDate.now());
        if (licenseAge < 1) {
            throw new InvalidBookingException("Driving License is issued less than a year");
        }
    }


    public CompletableFuture<RateResponse> calculateRentalPrice(BookingRequest request) {
        log.info("Calculating rental price for segment [{}]", request.carSegment());

        // Api Call
        Supplier<CompletableFuture<RateResponse>> pricingApiCall =
                () -> asyncApiCall(() -> pricingClient.getRate(
                        new RateRequest(request.carSegment().name())
                ));

        // Apply Retry + CircuitBreaker
        return applyResilience(
                pricingApiCall,
                () -> "Pricing Service temporarily unavailable",
                () -> request.carSegment().name(),
                RateResponse.class
        );
    }


    private <T> CompletableFuture<T> applyResilience(
            Supplier<CompletableFuture<T>> supplier,
            Supplier<String> unavailableMessage,
            Supplier<String> contextMessage,
            Class<T> responseClass) {

        // Decorate supplier with Retry (async)
        Supplier<CompletionStage<T>> retryStage =
                Retry.decorateCompletionStage(
                        externalApiRetry,
                        retryScheduler,
                        supplier::get
                );

        // Wrap retryStage with CircuitBreaker
        CompletionStage<T> circuitBreakerStage =
                externalApiCircuit.executeCompletionStage(retryStage);

        // Convert to CompletableFuture + fallback + Timeout
        return circuitBreakerStage
                .toCompletableFuture()
                .exceptionally(ex ->
                        handleFallback(ex, unavailableMessage, contextMessage)
                )
                .orTimeout(externalApiTimeout, TimeUnit.SECONDS);
    }


    private <T> CompletableFuture<T> asyncApiCall(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                T response = supplier.get();
                if (response == null) {
                    throw new ExternalApiRequestException("External API returned empty response");
                }
                return response;

            } catch (FeignException fe) {
                throw fe;
            } catch (Exception e) {
                throw new ExternalApiRequestException("API failed: " + e.getMessage(), e);
            }
        });
    }


    private <T> T handleFallback(Throwable throwable,
                                 Supplier<String> unavailableMessage,
                                 Supplier<String> context) {

        if (throwable instanceof InvalidBookingException ibe) throw ibe;
        if (throwable instanceof ExternalApiRequestException ex) throw ex;
        if (throwable instanceof FeignException fe) throw fe;

        log.error("Service unavailable for [{}]: {}", context.get(), throwable.getMessage());
        throw new ExternalApiUnavailableException(unavailableMessage.get());
    }


}
