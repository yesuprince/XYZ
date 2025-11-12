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
        CarInventory inventory = carInventoryRepository.lockInventoryRow(segment);
        if (inventory == null) {
            throw new IllegalArgumentException("Invalid car segment: " + segment);
        }

        // Check overlapping bookings
        int overlappingBookings = bookingRepository.countOverlappingBookings(segment, startDate, endDate);

        if (overlappingBookings >= inventory.getTotalCars()) {
            throw new IllegalStateException("No available cars for segment " + segment +
                    " between " + startDate + " and " + endDate);
        }
        bookingRepository.save(booking);
        return new BookingConfirmResponse(booking.getBookingId());
    }
}
