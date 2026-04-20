package com.example.gateway.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executor;

@Configuration
public class NacosDynamicConfig {

    private static final Logger logger = LoggerFactory.getLogger(NacosDynamicConfig.class);

    private static final String GATEWAY_CONFIG_DATA_ID = "gateway-dynamic-config";
    private static final String GATEWAY_CONFIG_GROUP = "SEKILL_GROUP";

    @Autowired
    private NacosConfigManager nacosConfigManager;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            String config = nacosConfigManager.getConfigService()
                    .getConfig(GATEWAY_CONFIG_DATA_ID, GATEWAY_CONFIG_GROUP, 5000);
            if (config != null) {
                logger.info("从Nacos加载初始配置: {}", config);
                applyConfiguration(config);
            }

            nacosConfigManager.getConfigService().addListener(
                    GATEWAY_CONFIG_DATA_ID,
                    GATEWAY_CONFIG_GROUP,
                    new Listener() {
                        @Override
                        public Executor getExecutor() {
                            return null;
                        }

                        @Override
                        public void receiveConfigInfo(String configInfo) {
                            logger.info("收到Nacos配置变更通知: {}", configInfo);
                            applyConfiguration(configInfo);
                        }
                    }
            );

            logger.info("Nacos动态配置监听器初始化完成");
        } catch (Exception e) {
            logger.warn("Nacos动态配置初始化失败，将使用默认配置: {}", e.getMessage());
        }
    }

    private void applyConfiguration(String config) {
        try {
            JsonNode root = objectMapper.readTree(config);

            if (root.has("circuitBreaker")) {
                JsonNode cbConfig = root.get("circuitBreaker");
                updateCircuitBreakerConfigs(cbConfig);
            }

            if (root.has("rateLimiter")) {
                JsonNode rlConfig = root.get("rateLimiter");
                updateRateLimiterConfigs(rlConfig);
            }

            logger.info("配置更新成功");
        } catch (Exception e) {
            logger.error("应用配置失败: {}", e.getMessage(), e);
        }
    }

    private void updateCircuitBreakerConfigs(JsonNode config) {
        config.fieldNames().forEachRemaining(name -> {
            try {
                JsonNode cbSettings = config.get(name);
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);

                if (cbSettings.has("failureRateThreshold")) {
                    float threshold = (float) cbSettings.get("failureRateThreshold").asDouble();
                    logger.info("更新CircuitBreaker {} 失败率阈值: {}", name, threshold);
                }

                if (cbSettings.has("waitDurationInOpenState")) {
                    long waitDuration = cbSettings.get("waitDurationInOpenState").asLong();
                    logger.info("更新CircuitBreaker {} 开启状态持续时间: {}ms", name, waitDuration);
                }

                if (cbSettings.has("slidingWindowSize")) {
                    int windowSize = cbSettings.get("slidingWindowSize").asInt();
                    logger.info("更新CircuitBreaker {} 滑动窗口大小: {}", name, windowSize);
                }

                logger.info("CircuitBreaker {} 配置已更新", name);
            } catch (Exception e) {
                logger.warn("更新CircuitBreaker {} 失败: {}", name, e.getMessage());
            }
        });
    }

    private void updateRateLimiterConfigs(JsonNode config) {
        config.fieldNames().forEachRemaining(name -> {
            try {
                JsonNode rlSettings = config.get(name);
                RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(name);

                if (rlSettings.has("limitForPeriod")) {
                    int limit = rlSettings.get("limitForPeriod").asInt();
                    logger.info("更新RateLimiter {} 限流阈值: {}/period", name, limit);
                }

                if (rlSettings.has("limitRefreshPeriod")) {
                    long period = rlSettings.get("limitRefreshPeriod").asLong();
                    logger.info("更新RateLimiter {} 刷新周期: {}ms", name, period);
                }

                logger.info("RateLimiter {} 配置已更新", name);
            } catch (Exception e) {
                logger.warn("更新RateLimiter {} 失败: {}", name, e.getMessage());
            }
        });
    }
}