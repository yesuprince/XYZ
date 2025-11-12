package com.xyz.booking.dto;

import com.xyz.booking.enums.CarSegment;

import java.time.LocalDate;

public record BookingDetailsResponse(
        String drivingLicenseNumber,
        String customerName,
        int customerAge,
        LocalDate reservationStartDate,
        LocalDate reservationEndDate,
        CarSegment carSegment,
        float rentalPrice
) {
}