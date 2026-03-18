package com.example.seckill_demo.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 布隆过滤器配置
 * 用于防止缓存穿透
 */
@Configuration
public class BloomFilterConfig {

    // 预计元素数量
    private static final int EXPECTED_INSERTIONS = 10000;
    // 期望的误判率
    private static final double FPP = 0.01;

    @Bean
    public BloomFilter<String> productBloomFilter() {
        // 创建布隆过滤器，用于过滤商品ID
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FPP
        );
    }
}
