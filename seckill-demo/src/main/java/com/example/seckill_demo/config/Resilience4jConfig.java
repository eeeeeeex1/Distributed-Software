package com.example.seckill_demo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j配置类
 * 实现限流、熔断、降级
 */
@Configuration
public class Resilience4jConfig {

    /**
     * 秒杀接口限流配置
     * 每秒最多100个请求
     */
    @Bean
    public RateLimiter seckillRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)              // 每个周期允许的请求数
                .limitRefreshPeriod(Duration.ofSeconds(1)) // 周期：1秒
                .timeoutDuration(Duration.ofSeconds(5))    // 等待获取许可的超时时间
                .build();
        
        return registry.rateLimiter("seckillRateLimiter", config);
    }
    
    /**
     * 秒杀服务熔断配置
     */
    @Bean
    public CircuitBreaker seckillCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)         // 失败率阈值：50%
                .slowCallRateThreshold(50)        // 慢调用率阈值：50%
                .slowCallDurationThreshold(Duration.ofSeconds(2)) // 慢调用时间阈值
                .waitDurationInOpenState(Duration.ofSeconds(10))  // 熔断器打开后等待时间
                .permittedNumberOfCallsInHalfOpenState(10)        // 半开状态允许的调用数
                .slidingWindowSize(100)           // 滑动窗口大小
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .minimumNumberOfCalls(10)         // 最小调用次数
                .build();
        
        return registry.circuitBreaker("seckillCircuitBreaker", config);
    }
    
    /**
     * 商品服务熔断配置
     */
    @Bean
    public CircuitBreaker productServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(30)
                .slowCallRateThreshold(30)
                .slowCallDurationThreshold(Duration.ofSeconds(1))
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowSize(50)
                .build();
        
        return registry.circuitBreaker("productService", config);
    }
}
