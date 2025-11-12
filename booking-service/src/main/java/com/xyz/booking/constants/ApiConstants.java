package com.xyz.booking.constants;

public final class ApiConstants {
    private ApiConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    public static final String V1 = "/api/v1";
    public static final String BOOKING_ID = "bookingId";

    public static final String BOOKING_BASE_PATH = "/bookings";
    public static final String BOOKING_CONFIRM_PATH = "/confirm";
    public static final String BOOKING_DETAILS_PATH = "/{" + BOOKING_ID + "}";


}
