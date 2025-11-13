package com.xyz.booking.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Aspect
@Component
public class ExecutionTimeAspect {
    
    @Around("within(com.xyz.booking.controller..*)")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {

        long start = System.currentTimeMillis();
        Object result;

        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            // Only log — DO NOT swallow or modify exception
            long end = System.currentTimeMillis();
            log.error("[{}] failed in {} ms with exception: {}",
                    pjp.getSignature().toShortString(),
                    (end - start),
                    ex.getClass().getSimpleName());

            throw ex;
        }

        if (result instanceof CompletableFuture<?> future) {
            return future.whenComplete((res, ex) -> {
                long end = System.currentTimeMillis();

                if (ex == null) {
                    log.info("[{}] executed in {} ms",
                            pjp.getSignature().toShortString(),
                            (end - start));
                } else {
                    log.error("[{}] failed in {} ms with exception: {}",
                            pjp.getSignature().toShortString(),
                            (end - start),
                            ex.getClass().getSimpleName());
                }
            });
        }

        long end = System.currentTimeMillis();
        log.info("[{}] executed in {} ms",
                pjp.getSignature().toShortString(),
                (end - start));

        return result;
    }
}
