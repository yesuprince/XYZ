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
        log.debug("Validating reservation duration for license={}", bookingRequest.drivingLicenseNumber());
        return CompletableFuture.runAsync(() -> {
            long reservationDays = ChronoUnit.DAYS.between(bookingRequest.reservationStartDate(), bookingRequest.reservationEndDate());
            if (reservationDays <= 0) {
                log.warn("Invalid reservation duration: end date before start date");
                throw new InvalidBookingException("Reservation end date must be after start date.");
            }
            if (reservationDays > properties.getMaxDays()) {
                log.warn("Reservation exceeds max allowed days={}", properties.getMaxDays());
                throw new InvalidBookingException("Reservation exceeds " + properties.getMaxDays() + " days.");
            }
        });
    }

    public CompletableFuture<LicenseResponse> validateLicense(BookingRequest bookingRequest) {
        log.info("Validating license [{}]", bookingRequest.drivingLicenseNumber());
        Supplier<CompletableFuture<LicenseResponse>> licenseSupplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                LicenseResponse licenseResponse = licenseClient.getLicenseDetails(new LicenseRequest(bookingRequest.drivingLicenseNumber()));
                if (licenseResponse == null) {
                    log.error("License API returned empty response");
                    throw new ExternalApiRequestException("License API returned empty response");
                }
                if (licenseResponse.expiryDate().isBefore(LocalDate.now())) {
                    log.warn("License [{}] is expired", bookingRequest.drivingLicenseNumber());
                    throw new InvalidBookingException("Driving License is expired");
                }
                long licenseAge = ChronoUnit.YEARS.between(licenseResponse.issueDate(), LocalDate.now());
                if (licenseAge < 1) {
                    log.warn("License [{}] issued less than a year ago", bookingRequest.drivingLicenseNumber());
                    throw new InvalidBookingException("Driving License is issued less than a year");
                }
                log.info("License [{}] validated successfully", bookingRequest.drivingLicenseNumber());
                return licenseResponse;
            } catch (FeignException fe) {
                throw fe;
            } catch (Exception e) {
                log.error("License API failure for [{}]: {}", bookingRequest.drivingLicenseNumber(), e.getMessage());
                throw new ExternalApiRequestException("License API failed: " + e.getMessage(), e);
            }
        });
        Supplier<CompletionStage<LicenseResponse>> licenseCompletion = Decorators.ofCompletionStage(licenseSupplier::get)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry, retryScheduler)
                .withFallback(throwable -> {
                    log.error("License service unavailable for [{}]", bookingRequest.drivingLicenseNumber());
                    throw new ExternalApiUnavailableException("License Service temporarily unavailable");
                })
                .decorate();
        return licenseCompletion.get()
                .toCompletableFuture()
                .orTimeout(externalApiTimeout, TimeUnit.SECONDS);
    }

    public CompletableFuture<RateResponse> calculateRentalPrice(BookingRequest bookingRequest) {
        log.info("Calculating rental price for segment [{}]", bookingRequest.carSegment());

        Supplier<CompletableFuture<RateResponse>> rateSupplier = () -> CompletableFuture.supplyAsync(() -> {
            try {
                RateResponse rateResponse = pricingClient.getRate(new RateRequest(bookingRequest.carSegment().name()));
                if (rateResponse == null) {
                    throw new ExternalApiRequestException("Pricing API returned empty response");
                }
                log.info("Pricing retrieved successfully for segment [{}]", bookingRequest.carSegment());
                return rateResponse;
            } catch (FeignException fe) {
                throw fe;
            } catch (Exception e) {
                log.error("Pricing API failure for segment [{}]: {}", bookingRequest.carSegment(), e.getMessage());
                throw new ExternalApiRequestException("Pricing API failed: " + e.getMessage(), e);
            }
        });
        Supplier<CompletionStage<RateResponse>> rateCompletion = Decorators.ofCompletionStage(rateSupplier::get)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry, retryScheduler)
                .withFallback(throwable -> {
                    log.error("Pricing service unavailable for segment [{}]", bookingRequest.carSegment());
                    throw new ExternalApiUnavailableException("Pricing Service temporarily unavailable");
                })
                .decorate();
        return rateCompletion.get()
                .toCompletableFuture()
                .orTimeout(externalApiTimeout, TimeUnit.SECONDS);
    }


}
