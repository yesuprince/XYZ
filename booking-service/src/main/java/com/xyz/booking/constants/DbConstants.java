package com.xyz.booking.constants;

public class DbConstants {
    private DbConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
    public static final String TABLE_BOOKING = "booking";
    public static final String TABLE_CAR_INVENTORY = "car_inventory";
    public static final String COLUMN_BOOKING_ID = "booking_id";
}
