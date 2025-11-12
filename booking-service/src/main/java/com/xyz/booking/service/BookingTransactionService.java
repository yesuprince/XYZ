package com.xyz.booking.service;

import com.xyz.booking.dto.BookingConfirmResponse;
import com.xyz.booking.entity.Booking;
import com.xyz.booking.entity.CarInventory;
import com.xyz.booking.enums.CarSegment;
import com.xyz.booking.repository.BookingRepository;
import com.xyz.booking.repository.CarInventoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingTransactionService {
    private final BookingRepository bookingRepository;
    private final CarInventoryRepository carInventoryRepository;

    @Transactional
    public BookingConfirmResponse saveBooking(Booking booking) {
        CarSegment segment = booking.getCarSegment();
        LocalDate startDate = booking.getStartDate();
        LocalDate endDate = booking.getEndDate();
        // Lock inventory row for this segment
        log.debug("Attempting to lock inventory for segment [{}]", segment);
        CarInventory inventory = carInventoryRepository.lockInventoryRow(segment);

        if (inventory == null) {
            log.error("Inventory lock failed: Invalid car segment [{}]", segment);
            throw new IllegalArgumentException("Invalid car segment: " + segment);
        }
        log.debug("Inventory locked successfully for segment [{}] with totalCars={}", segment, inventory.getTotalCars());


        // Check overlapping bookings
        log.debug("Checking for overlapping bookings in segment [{}] between [{}] and [{}]", segment, startDate, endDate);
        int overlappingBookings = bookingRepository.countOverlappingBookings(segment, startDate, endDate);

        log.debug("Found [{}] overlapping bookings for segment [{}]", overlappingBookings, segment);

        if (overlappingBookings >= inventory.getTotalCars()) {
            log.warn("No cars available for segment [{}] between [{}] and [{}] ({} overlapping, {} total)",
                    segment, startDate, endDate, overlappingBookings, inventory.getTotalCars());
            throw new IllegalStateException("No available cars for segment " + segment +
                    " between " + startDate + " and " + endDate);
        }
        bookingRepository.save(booking);
        log.info("Booking saved successfully with bookingId [{}] for license [{}]", booking.getBookingId(), booking.getLicenseNumber());
        return new BookingConfirmResponse(booking.getBookingId());
    }
}
