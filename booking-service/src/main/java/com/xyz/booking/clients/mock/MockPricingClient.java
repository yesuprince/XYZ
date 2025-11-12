package com.xyz.booking.clients.mock;

import com.xyz.booking.clients.PricingClient;
import com.xyz.booking.constants.Profiles;
import com.xyz.booking.dto.RateRequest;
import com.xyz.booking.dto.RateResponse;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Profile(Profiles.MOCK)
@Component
public class MockPricingClient implements PricingClient {

    private static final Map<String, Float> RATES = Map.of(
            "SMALL", 25.0f,
            "MEDIUM", 45.99f,
            "LARGE", 65.5f,
            "EXTRA_LARGE", 99.99f
    );

    private static final Request DUMMY_REQUEST = Request.create(
            Request.HttpMethod.POST,
            "/rental/rate",
            Collections.emptyMap(),
            null,
            StandardCharsets.UTF_8,
            new RequestTemplate()
    );

    @Override
    public RateResponse getRate(RateRequest request) {
        String category = request.category().toUpperCase();

        return switch (category) {
            case "INVALID" -> throw FeignException.errorStatus(
                    "getRate",
                    feign.Response.builder()
                            .status(400)
                            .reason("Bad Request")
                            .request(DUMMY_REQUEST)
                            .body("{\"error\":\"Invalid car category\"}", StandardCharsets.UTF_8)
                            .build()
            );

            case "SERVER_ERR" -> throw FeignException.errorStatus(
                    "getRate",
                    feign.Response.builder()
                            .status(500)
                            .reason("Internal Server Error")
                            .request(DUMMY_REQUEST)
                            .body("{\"error\":\"Internal server error\"}", StandardCharsets.UTF_8)
                            .build()
            );

            default -> {
                Float rate = RATES.get(category);
                if (rate == null) {
                    throw FeignException.errorStatus(
                            "getRate",
                            feign.Response.builder()
                                    .status(400)
                                    .reason("Bad Request")
                                    .request(DUMMY_REQUEST)
                                    .body("{\"error\":\"Invalid car category\"}", StandardCharsets.UTF_8)
                                    .build()
                    );
                }
                yield new RateResponse(category, rate);
            }
        };
    }
}
