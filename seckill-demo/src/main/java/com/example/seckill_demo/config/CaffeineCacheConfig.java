package com.example.seckill_demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine本地缓存配置
 * 实现多级缓存架构
 */
@Configuration
public class CaffeineCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("products");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 最大缓存数量
                .maximumSize(1000)
                // 过期时间（5分钟）
                .expireAfterWrite(5, TimeUnit.MINUTES)
        );
        return cacheManager;
    }
}
