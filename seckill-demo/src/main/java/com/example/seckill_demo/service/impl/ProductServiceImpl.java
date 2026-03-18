package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.entity.Product;
import com.example.seckill_demo.mapper.ProductMapper;
import com.example.seckill_demo.service.ProductService;
import com.google.common.hash.BloomFilter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类（含完整缓存防护机制）
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private BloomFilter<String> productBloomFilter;

    // 缓存key前缀
    private static final String CACHE_KEY_PREFIX = "product:detail:";
    // 缓存过期时间（30分钟）+ 随机偏移（避免雪崩）
    private static final long BASE_TTL = 30;
    private static final long RANDOM_TTL = 10;
    // 热点商品标记
    private static final String HOT_PRODUCT_PREFIX = "hot:product:";

    @Override
    @Cacheable(value = "products", key = "#productId")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductDetailFallback")
    public Product getProductDetail(Long productId) {
        if (productId == null || productId <= 0) {
            return null;
        }

        String productIdStr = String.valueOf(productId);
        
        // 1. 布隆过滤器过滤不存在的商品（防止穿透）
        if (!productBloomFilter.mightContain(productIdStr)) {
            return null;
        }

        String key = CACHE_KEY_PREFIX + productId;
        
        // 2. 先查Redis缓存
        Product product = (Product) redisTemplate.opsForValue().get(key);
        if (product != null) {
            // 缓存命中：如果是空值（解决穿透），返回null
            if (product.getId() == null) {
                return null;
            }
            return product;
        }

        // 3. 缓存未命中：加分布式锁（解决击穿）
        String lockKey = "lock:product:" + productId;
        try {
            Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
            if (lock != null && lock) {
                // 4. 锁成功：查数据库
                product = productMapper.selectById(productId);
                if (product != null) {
                    // 检查是否为热点商品
                    boolean isHotProduct = checkHotProduct(productId);
                    if (isHotProduct) {
                        // 热点商品：永不过期策略
                        redisTemplate.opsForValue().set(key, product);
                        redisTemplate.opsForValue().set(HOT_PRODUCT_PREFIX + productId, "1");
                    } else {
                        // 普通商品：加随机过期时间（避免雪崩）
                        long ttl = BASE_TTL + (long) (Math.random() * RANDOM_TTL);
                        redisTemplate.opsForValue().set(key, product, ttl, TimeUnit.MINUTES);
                    }
                    // 将商品ID加入布隆过滤器
                    productBloomFilter.put(productIdStr);
                } else {
                    // 缓存空值（解决穿透）：短期缓存（5分钟）
                    redisTemplate.opsForValue().set(key, new Product(), 5, TimeUnit.MINUTES);
                }
                return product;
            } else {
                // 5. 锁失败：重试查缓存（避免并发重建）
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

    /**
     * 熔断降级方法
     */
    public Product getProductDetailFallback(Long productId, Throwable throwable) {
        // 降级策略：返回默认商品信息或空
        System.err.println("Product service circuit breaker triggered: " + throwable.getMessage());
        return null;
    }

    /**
     * 检查是否为热点商品
     */
    private boolean checkHotProduct(Long productId) {
        // 简单实现：可以根据访问频率、库存等判断
        // 这里模拟热点商品判断
        return productId <= 10; // 假设ID<=10的是热点商品
    }

    /**
     * 更新商品时同步更新缓存
     */
    public void updateProductCache(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        
        String key = CACHE_KEY_PREFIX + product.getId();
        // 更新缓存
        redisTemplate.opsForValue().set(key, product);
        // 将商品ID加入布隆过滤器
        productBloomFilter.put(String.valueOf(product.getId()));
    }
}