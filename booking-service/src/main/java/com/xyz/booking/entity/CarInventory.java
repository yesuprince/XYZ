package com.xyz.booking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import static com.xyz.booking.constants.DbConstants.TABLE_CAR_INVENTORY;

@Entity
@Getter
@Setter
@Table(name = TABLE_CAR_INVENTORY)
public class CarInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String carSegment;

    @Column(nullable = false)
    private int totalCars;
}
