package com.xyz.booking.service;

import com.xyz.booking.dto.*;
import com.xyz.booking.entity.Booking;
import com.xyz.booking.exception.BookingNotFoundException;
import com.xyz.booking.exception.InvalidBookingException;
import com.xyz.booking.repository.BookingRepository;
import com.xyz.booking.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class BookingService {
    private final BookingTransactionService bookingTransactionService;
    private final BookingRepository bookingRepository;
    private final Validator validator;
    private final Executor bookingExecutor;

    public BookingService(BookingTransactionService bookingTransactionService, BookingRepository bookingRepository, Validator validator, @Qualifier("bookingExecutor") Executor bookingExecutor) {
        this.bookingTransactionService = bookingTransactionService;
        this.bookingRepository = bookingRepository;
        this.validator = validator;
        this.bookingExecutor = bookingExecutor;
    }

    public CompletableFuture<BookingConfirmResponse> book(BookingRequest bookingRequest) {
        log.info("Initiating booking for license [{}], customer age [{}], car segment [{}]",
                bookingRequest.drivingLicenseNumber(),
                bookingRequest.customerAge(),
                bookingRequest.carSegment());
        CompletableFuture<Void> validReservation = validator.validateReservationDuration(bookingRequest);
        CompletableFuture<LicenseResponse> licenseFuture = validator.validateLicense(bookingRequest);
        CompletableFuture<RateResponse> rateFuture = validator.calculateRentalPrice(bookingRequest);
        return validReservation.thenCombineAsync(licenseFuture, (valid, license) -> license, bookingExecutor)
                .thenCombineAsync(rateFuture, (license, rate) -> {
                    long reservedDays = ChronoUnit.DAYS.between(bookingRequest.reservationStartDate(), bookingRequest.reservationEndDate());
                    float totalRate = reservedDays * rate.ratePerDay();
                    log.info("Booking computed for license [{}]: reservedDays={}, ratePerDay={}, totalRate={}",
                            bookingRequest.drivingLicenseNumber(), reservedDays, rate.ratePerDay(), totalRate);

                    Booking booking = createBookingEntity(bookingRequest, license, totalRate);

                    return CompletableFuture.supplyAsync(
                            () -> {
                                log.info("Saving booking for license [{}] in transaction service", bookingRequest.drivingLicenseNumber());
                                return bookingTransactionService.saveBooking(booking);
                            }, bookingExecutor);
                }, bookingExecutor)
                .thenCompose(future -> future)
                .exceptionally(ex -> {
                    log.error("Booking failed for license [{}]: {}", bookingRequest.drivingLicenseNumber(), ex.getMessage(), ex);
                    throw unwrapAndRethrow(ex);
                });
    }

    private static Booking createBookingEntity(BookingRequest bookingRequest, LicenseResponse license, float totalRate) {
        Booking booking = new Booking();
        booking.setCustomerName(license.ownerName());
        booking.setAge(bookingRequest.customerAge());
        booking.setLicenseNumber(bookingRequest.drivingLicenseNumber());
        booking.setCarSegment(bookingRequest.carSegment());
        booking.setStartDate(bookingRequest.reservationStartDate());
        booking.setEndDate(bookingRequest.reservationEndDate());
        booking.setRentalPrice(totalRate);
        return booking;
    }

    private static RuntimeException unwrapAndRethrow(Throwable ex) {
        Throwable cause = (ex instanceof CompletionException && ex.getCause() != null)
                ? ex.getCause()
                : ex;
        if (cause instanceof RuntimeException re) return re;
        return new RuntimeException(cause);
    }

    public CompletableFuture<BookingDetailsResponse> getBookingDetail(Long bookingId) {
        log.info("Fetching booking details for bookingId={}", bookingId);
        return CompletableFuture.supplyAsync(() -> {
            if (null == bookingId) {
                log.warn("Invalid booking request: bookingId is null");
                throw new InvalidBookingException("Booking Id is null");
            }
            Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> {
                log.warn("No booking found for bookingId={}", bookingId);
                return new BookingNotFoundException("No booking found for the Id");
            });
            log.info("Booking details retrieved successfully for bookingId={}", bookingId);
            return new BookingDetailsResponse(
                    booking.getLicenseNumber(),
                    booking.getCustomerName(),
                    booking.getAge(),
                    booking.getStartDate(),
                    booking.getEndDate(),
                    booking.getCarSegment(),
                    booking.getRentalPrice());
        }, bookingExecutor).exceptionally(ex -> {
            log.error("Error while fetching booking details for bookingId={}: {}", bookingId, ex.getMessage(), ex);
            throw unwrapAndRethrow(ex);
        });
    }
}

