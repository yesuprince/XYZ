package com.xyz.booking.config;

import com.xyz.booking.exception.ExternalApiRequestException;
import com.xyz.booking.exception.InvalidBookingException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ResilienceConfig {

    @Bean
    public Retry externalApiRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(300))
                .ignoreExceptions(
                        InvalidBookingException.class,
                        ExternalApiRequestException.class,
                        FeignException.class
                )
                .build();
        return Retry.of("externalApiRetry", config);
    }

    @Bean
    public CircuitBreaker externalApiCircuit() {
        CircuitBreakerConfig cb = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .minimumNumberOfCalls(10)
                .slidingWindowSize(20)
                .permittedNumberOfCallsInHalfOpenState(3)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .build();
        return CircuitBreaker.of("externalApiCircuit", cb);
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService retryScheduler() {
        return Executors.newScheduledThreadPool(5);
    }
}
