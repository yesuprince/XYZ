package com.xyz.booking.validation;

import com.xyz.booking.clients.LicenseClient;
import com.xyz.booking.clients.PricingClient;
import com.xyz.booking.dto.*;
import com.xyz.booking.exception.ExternalApiException;
import com.xyz.booking.exception.InvalidBookingException;
import com.xyz.booking.properties.ValidationProperties;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
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
    private final Retry retry;
    private final ScheduledExecutorService retryScheduler;
    private final CircuitBreaker circuitBreaker;
    private final LicenseClient licenseClient;
    private final PricingClient pricingClient;
    private final ValidationProperties properties;
    @Value("${external.apis.timeout}")
    private long externalApiTimeout;

    public CompletableFuture<Void> validateReservationDuration(BookingRequest bookingRequest) {
        return CompletableFuture.runAsync(() -> {
            long reservationDays = ChronoUnit.DAYS.between(bookingRequest.reservationStartDate(), bookingRequest.reservationEndDate());
            if (reservationDays <= 0) {
                throw new InvalidBookingException("Reservation end date must be after start date.");
            }
            if (reservationDays > properties.getMaxDays()) {
                throw new InvalidBookingException("Reservation exceeds " + properties.getMaxDays() + " days.");
            }
        });
    }

    public CompletableFuture<LicenseResponse> validateLicense(BookingRequest bookingRequest) {
        Supplier<CompletableFuture<LicenseResponse>> licenseSupplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                LicenseResponse licenseResponse = licenseClient.getLicenseDetails(new LicenseRequest(bookingRequest.drivingLicenseNumber()));
                if (licenseResponse == null) {
                    throw new ExternalApiException("License API returned empty response");
                }
                if (licenseResponse.expiryDate().isBefore(LocalDate.now())) {
                    throw new InvalidBookingException("Driving License is expired");
                }
                long licenseAge = ChronoUnit.YEARS.between(licenseResponse.issueDate(), LocalDate.now());
                if (licenseAge < 1) {
                    throw new InvalidBookingException("Driving License is issued less than a year");
                }
                return licenseResponse;
            } catch (FeignException fe) {
                throw fe;
            } catch (Exception e) {
                throw new ExternalApiException("License API failed: " + e.getMessage(), e);
            }
        });
        Supplier<CompletionStage<LicenseResponse>> licenseCompletion = Decorators.ofCompletionStage(licenseSupplier::get)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry, retryScheduler)
                .decorate();
        return licenseCompletion.get()
                .toCompletableFuture()
                .orTimeout(externalApiTimeout, TimeUnit.SECONDS);
    }

    public CompletableFuture<RateResponse> calculateRentalPrice(BookingRequest bookingRequest) {
        Supplier<CompletableFuture<RateResponse>> rateSupplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                RateResponse rateResponse = pricingClient.getRate(new RateRequest(bookingRequest.carSegment().name()));
                if (rateResponse == null) {
                    throw new ExternalApiException("Pricing API returned empty response");
                }
                return rateResponse;
            } catch (FeignException fe) {
                throw fe;
            } catch (Exception e) {
                throw new ExternalApiException("Pricing API failed: " + e.getMessage(), e);
            }
        });
        Supplier<CompletionStage<RateResponse>> rateCompletion = Decorators.ofCompletionStage(rateSupplier::get)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry, retryScheduler)
                .decorate();
        return rateCompletion.get()
                .toCompletableFuture()
                .orTimeout(externalApiTimeout, TimeUnit.SECONDS);
    }


}
