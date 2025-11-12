package com.xyz.booking.dto;

import com.xyz.booking.enums.CarSegment;

import java.time.LocalDate;

public record BookingRequest(
        String drivingLicenseNumber,
        int customerAge,
        LocalDate reservationStartDate,
        LocalDate reservationEndDate,
        CarSegment carSegment
) {
}
