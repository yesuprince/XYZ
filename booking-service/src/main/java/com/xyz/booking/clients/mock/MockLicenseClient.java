package com.xyz.booking.clients.mock;

import com.xyz.booking.clients.LicenseClient;
import com.xyz.booking.constants.Profiles;
import com.xyz.booking.dto.LicenseRequest;
import com.xyz.booking.dto.LicenseResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;

@Profile(Profiles.MOCK)
@Component
public class MockLicenseClient implements LicenseClient {

    private static final Request DUMMY_REQUEST =
            Request.create(Request.HttpMethod.POST, "/license/details",
                    Collections.emptyMap(), null, StandardCharsets.UTF_8, new RequestTemplate());

    @Override
    public LicenseResponse getLicenseDetails(LicenseRequest request) {
        String license = request.licenseNumber().toUpperCase();

        return switch (license) {
            case "RECENT" -> new LicenseResponse("John Doe (" + license + ")",
                    LocalDate.now().minusMonths(5),
                    LocalDate.parse("2025-12-31"));
            case "EXPIRED" -> new LicenseResponse("John Doe (" + license + ")",
                    LocalDate.now().minusYears(20),
                    LocalDate.now().minusMonths(1));
            case "BAD" -> throw FeignException.errorStatus("getLicenseDetails",
                    feign.Response.builder()
                            .status(400)
                            .reason("Bad Request")
                            .request(DUMMY_REQUEST)
                            .body("{\"error\":\"Invalid license number\"}", StandardCharsets.UTF_8)
                            .build());
            case "NOTFOUND" -> throw FeignException.errorStatus("getLicenseDetails",
                    feign.Response.builder()
                            .status(404)
                            .reason("Not Found")
                            .request(DUMMY_REQUEST)
                            .body("{\"error\":\"Driving license not found\"}", StandardCharsets.UTF_8)
                            .build());
            case "ERROR" -> throw FeignException.errorStatus("getLicenseDetails",
                    feign.Response.builder()
                            .status(500)
                            .reason("Internal Server Error")
                            .request(DUMMY_REQUEST)
                            .body("{\"error\":\"Internal server error\"}", StandardCharsets.UTF_8)
                            .build());
            default -> new LicenseResponse("John Doe (" + license + ")",
                    LocalDate.now().minusYears(10),
                    LocalDate.now().plusYears(10));
        };
    }
}
