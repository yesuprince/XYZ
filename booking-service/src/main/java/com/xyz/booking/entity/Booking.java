package com.xyz.booking.entity;

import com.xyz.booking.enums.CarSegment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

import static com.xyz.booking.constants.DbConstants.COLUMN_BOOKING_ID;
import static com.xyz.booking.constants.DbConstants.TABLE_BOOKING;

@Entity
@Getter
@Setter
@Table(name = TABLE_BOOKING)
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = COLUMN_BOOKING_ID, updatable = false, nullable = false)
    private Long bookingId;
    private String customerName;
    private int age;
    private String licenseNumber;
    @Enumerated(EnumType.STRING)
    private CarSegment carSegment;
    private LocalDate startDate;
    private LocalDate endDate;
    private float rentalPrice;
}
