package com.xyz.booking.service;

import com.xyz.booking.dto.BookingDetailsResponse;
import com.xyz.booking.dto.BookingRequest;
import com.xyz.booking.entity.Booking;
import com.xyz.booking.enums.CarSegment;
import com.xyz.booking.exception.BookingNotFoundException;
import com.xyz.booking.exception.InvalidBookingException;
import com.xyz.booking.repository.BookingRepository;
import com.xyz.booking.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BookingServiceTest {

    @Mock
    private BookingTransactionService bookingTransactionService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private Validator validator;

    private BookingService bookingService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        Executor directExecutor = Runnable::run;

        bookingService = new BookingService(
                bookingTransactionService,
                bookingRepository,
                validator,
                directExecutor
        );
    }

    private BookingRequest createRequest() {
        return new BookingRequest(
                "DL123",
                30,
                LocalDate.now(),
                LocalDate.now().plusDays(4), CarSegment.MEDIUM
        );
    }


    @Test
    void testGetBookingDetail_success() throws Exception {

        Booking booking = new Booking();
        booking.setLicenseNumber("DL123");
        booking.setCustomerName("John Doe");
        booking.setAge(30);
        booking.setStartDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(2));
        booking.setCarSegment(CarSegment.MEDIUM);
        booking.setRentalPrice(200f);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        CompletableFuture<BookingDetailsResponse> future =
                bookingService.getBookingDetail(1L);

        BookingDetailsResponse response = future.get();

        assertNotNull(response);
        assertEquals("DL123", response.drivingLicenseNumber());
        assertEquals("John Doe", response.customerName());
    }


    @Test
    void testGetBookingDetail_nullId() {

        CompletableFuture<BookingDetailsResponse> future =
                bookingService.getBookingDetail(null);

        CompletionException ex =
                assertThrows(CompletionException.class, future::join);
        assertInstanceOf(InvalidBookingException.class, ex.getCause());

        assertEquals("Booking Id is null", ex.getCause().getMessage());
    }


    @Test
    void testGetBookingDetail_notFound() {

        when(bookingRepository.findById(55L))
                .thenReturn(Optional.empty());

        CompletableFuture<BookingDetailsResponse> future =
                bookingService.getBookingDetail(55L);

        CompletionException completionEx =
                assertThrows(CompletionException.class, future::join);


        assertInstanceOf(BookingNotFoundException.class, completionEx.getCause());
        assertEquals("No booking found for the Id", completionEx.getCause().getMessage());
    }
}