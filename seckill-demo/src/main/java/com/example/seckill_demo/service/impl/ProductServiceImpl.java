package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.entity.Product;
import com.example.seckill_demo.mapper.ProductMapper;
import com.example.seckill_demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类（含Redis缓存：解决穿透/击穿/雪崩）
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductMapper productMapper;

    // 缓存key前缀
    private static final String CACHE_KEY_PREFIX = "product:detail:";
    // 缓存过期时间（30分钟）+ 随机偏移（避免雪崩）
    private static final long BASE_TTL = 30;
    private static final long RANDOM_TTL = 10;

    @Override
    public Product getProductDetail(Long productId) {
        if (productId == null || productId <= 0) {
            return null;
        }

        String key = CACHE_KEY_PREFIX + productId;
        // 1. 先查缓存
        Product product = (Product) redisTemplate.opsForValue().get(key);
        if (product != null) {
            // 缓存命中：如果是空值（解决穿透），返回null
            if (product.getId() == null) {
                return null;
            }
            return product;
        }

        // 2. 缓存未命中：加分布式锁（解决击穿）
        String lockKey = "lock:product:" + productId;
        try {
            Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
            if (lock != null && lock) {
                // 3. 锁成功：查数据库
                product = productMapper.selectById(productId);
                if (product != null) {
                    // 写入缓存：加随机过期时间（避免雪崩）
                    long ttl = BASE_TTL + (long) (Math.random() * RANDOM_TTL);
                    redisTemplate.opsForValue().set(key, product, ttl, TimeUnit.MINUTES);
                } else {
                    // 缓存空值（解决穿透）：短期缓存（5分钟）
                    redisTemplate.opsForValue().set(key, new Product(), 5, TimeUnit.MINUTES);
                }
                return product;
            } else {
                // 4. 锁失败：重试查缓存（避免并发重建）
                Thread.sleep(50);
                return getProductDetail(productId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }
}