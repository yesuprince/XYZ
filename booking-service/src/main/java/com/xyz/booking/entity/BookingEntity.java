package com.xyz.booking.entity;

import com.xyz.booking.enums.CarType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

import static com.xyz.booking.constants.DbConstants.COLUMN_BOOKING_ID;
import static com.xyz.booking.constants.DbConstants.TABLE_BOOKING;

@Entity
@Table(name = TABLE_BOOKING)
public class BookingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = COLUMN_BOOKING_ID, updatable = false, nullable = false)
    private Long bookingId;
    private String customerId;
    private int age;
    @Enumerated(EnumType.ORDINAL)
    private CarType carType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
