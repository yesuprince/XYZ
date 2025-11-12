package com.xyz.booking.dto;

import java.time.LocalDate;

public record LicenseResponse(String ownerName, LocalDate issueDate, LocalDate expiryDate) {
}

