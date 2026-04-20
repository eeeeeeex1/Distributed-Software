package com.example.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.cloud.nacos.NacosConfigManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class Resilience4jConfig {

    @Autowired(required = false)
    private NacosConfigManager nacosConfigManager;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()      
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(IOException.class, TimeoutException.class, RuntimeException.class)
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        return RateLimiterRegistry.of(defaultConfig);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(IOException.class, TimeoutException.class)     
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    @PostConstruct
    public void init() {
        createCircuitBreaker("seckillCircuitBreaker", 10, 50, Duration.ofSeconds(10));
        createCircuitBreaker("paymentCircuitBreaker", 8, 60, Duration.ofSeconds(15));
        createCircuitBreaker("stockCircuitBreaker", 12, 40, Duration.ofSeconds(8));

        createRateLimiter("seckillRateLimiter", 100, Duration.ofSeconds(1));    
        createRateLimiter("paymentRateLimiter", 50, Duration.ofSeconds(1));     
        createRateLimiter("stockRateLimiter", 200, Duration.ofSeconds(1));      

        createRetry("seckillRetry", 2, Duration.ofMillis(500));
        createRetry("paymentRetry", 3, Duration.ofMillis(500));
        createRetry("stockRetry", 2, Duration.ofMillis(300));
    }

    private CircuitBreaker createCircuitBreaker(String name, int slidingWindowSize, int failureRateThreshold, Duration waitDuration) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(waitDuration)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(IOException.class, TimeoutException.class, RuntimeException.class)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of(name, config);        

        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        System.out.println("CircuitBreaker " + name + " state changed: " + event.getStateTransition()))
                .onFailureRateExceeded(event ->
                        System.out.println("CircuitBreaker " + name + " failure rate exceeded: " + event.getFailureRate()))
                .onCallNotPermitted(event ->
                        System.out.println("CircuitBreaker " + name + " call not permitted"));

        return circuitBreaker;
    }

    private RateLimiter createRateLimiter(String name, int limitForPeriod, Duration limitRefreshPeriod) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(limitRefreshPeriod)
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        RateLimiter rateLimiter = RateLimiter.of(name, config);

        rateLimiter.getEventPublisher()
                .onSuccess(event ->
                        System.out.println("RateLimiter " + name + " request succeeded"))
                .onFailure(event ->
                        System.out.println("RateLimiter " + name + " request rejected"));

        return rateLimiter;
    }

    private Retry createRetry(String name, int maxAttempts, Duration waitDuration) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(waitDuration)
                .retryExceptions(IOException.class, TimeoutException.class, RuntimeException.class)
                .build();

        Retry retry = Retry.of(name, config);

        retry.getEventPublisher()
                .onRetry(event ->
                        System.out.println("Retry " + name + " attempt " + event.getNumberOfRetryAttempts()))
                .onError(event ->
                        System.out.println("Retry " + name + " failed after " + event.getNumberOfRetryAttempts() + " attempts"));

        return retry;
    }
}