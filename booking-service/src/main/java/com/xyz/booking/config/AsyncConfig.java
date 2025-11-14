package com.xyz.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AsyncConfig {

    @Bean("bookingExecutor")
    public Executor bookingExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(3);
        exec.setMaxPoolSize(5);
        exec.setQueueCapacity(50);
        exec.setKeepAliveSeconds(30);
        exec.setAllowCoreThreadTimeOut(false);
        exec.setThreadNamePrefix("booking-exec-");
        exec.initialize();
        return exec;
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService retryScheduler() {
        return Executors.newScheduledThreadPool(5);
    }
}
