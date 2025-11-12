package com.xyz.booking.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "booking.validation")
public class ValidationProperties {
    private int maxDays;
    private int minLicenseAgeYears;
}
