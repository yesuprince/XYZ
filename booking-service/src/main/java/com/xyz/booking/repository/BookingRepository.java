package com.xyz.booking.repository;

import com.xyz.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("""
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.carSegment = :segment
                  AND b.startDate <= :expectedEndDate
                  AND b.endDate >= :expectedStartDate
            """)
    int countOverlappingBookings(String segment, LocalDate expectedStartDate, LocalDate expectedEndDate);
}
