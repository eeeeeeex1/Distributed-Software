package com.example.seckill_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;

/**
 * Redis配置类：解决序列化问题，统一Redis模板
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置连接工厂
        redisTemplate.setConnectionFactory(factory);
        
        // 配置key序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        
        // 配置value序列化（使用JDK序列化）
        JdkSerializationRedisSerializer jdkSerializer = new JdkSerializationRedisSerializer();
        redisTemplate.setValueSerializer(jdkSerializer);
        redisTemplate.setHashValueSerializer(jdkSerializer);
        
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}