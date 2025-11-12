package com.xyz.booking.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "resilience4j")
public class ResilienceProperties {
    private RetryProperties retry = new RetryProperties();
    private TimeLimiterProperties timelimiter = new TimeLimiterProperties();
    private CircuitBreakerProperties circuitbreaker = new CircuitBreakerProperties();

    @Getter
    @Setter
    public static class RetryProperties {
        private Map<String, Object> instances;
    }

    @Getter
    @Setter
    public static class TimeLimiterProperties {
        private Map<String, Object> instances;
    }

    @Getter
    @Setter
    public static class CircuitBreakerProperties {
        private Map<String, Object> instances;
    }
}
