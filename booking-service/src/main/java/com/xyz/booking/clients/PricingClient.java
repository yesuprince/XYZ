package com.xyz.booking.clients;

import com.xyz.booking.constants.Profiles;
import com.xyz.booking.dto.RateRequest;
import com.xyz.booking.dto.RateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "pricingClient", url = "${external.apis.pricing-service-url}")
@Profile(Profiles.PROD)
public interface PricingClient {
    @PostMapping("/rental/rate")
    RateResponse getRate(@RequestBody RateRequest request);
}
