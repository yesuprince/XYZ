package com.xyz.booking.clients;

import com.xyz.booking.constants.Profiles;
import com.xyz.booking.dto.LicenseRequest;
import com.xyz.booking.dto.LicenseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "licenseClient", url = "${external.apis.license-service-url}")
@Profile(Profiles.PROD)
public interface LicenseClient {
    @PostMapping("/license/details")
    LicenseResponse getLicenseDetails(@RequestBody LicenseRequest request);
}
