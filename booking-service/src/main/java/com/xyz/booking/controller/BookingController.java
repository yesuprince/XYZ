package com.xyz.booking.controller;

import com.xyz.booking.dto.BookingConfirmResponse;
import com.xyz.booking.dto.BookingDetailsResponse;
import com.xyz.booking.dto.BookingRequest;
import com.xyz.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

import static com.xyz.booking.constants.ApiConstants.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(V1 + BOOKING_BASE_PATH)
public class BookingController {
    private final BookingService bookingService;

    @PostMapping(path = BOOKING_CONFIRM_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<BookingConfirmResponse>> confirmBooking(@RequestBody BookingRequest bookingRequest) {
        return bookingService.book(bookingRequest).thenApply(ResponseEntity::ok);
    }

    @GetMapping(path = BOOKING_DETAILS_PATH)
    public CompletableFuture<ResponseEntity<BookingDetailsResponse>> getBookingDetails(@PathVariable(BOOKING_ID) Long bookingId) {
        return bookingService.getBookingDetail(bookingId).thenApply(ResponseEntity::ok);
    }


}
