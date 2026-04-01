package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.mapper.ProductMapper;
import com.example.seckill_demo.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 库存服务实现类
 * 使用Redis + Lua脚本实现原子性库存操作
 */
@Service
public class StockServiceImpl implements StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockServiceImpl.class);
    
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String STOCK_LOCK_PREFIX = "seckill:stock:lock:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProductMapper productMapper;
    
    // Lua脚本：原子性扣减库存
    private static final String DEDUCT_STOCK_SCRIPT = 
            "local stockKey = KEYS[1]\n" +
            "local quantity = tonumber(ARGV[1])\n" +
            "local currentStock = tonumber(redis.call('GET', stockKey))\n" +
            "if currentStock == nil then\n" +
            "    return -2\n" +
            "end\n" +
            "if currentStock >= quantity then\n" +
            "    return redis.call('DECRBY', stockKey, quantity)\n" +
            "else\n" +
            "    return -1\n" +
            "end";
    
    // Lua脚本：原子性回滚库存
    private static final String ROLLBACK_STOCK_SCRIPT = 
            "local stockKey = KEYS[1]\n" +
            "local quantity = tonumber(ARGV[1])\n" +
            "return redis.call('INCRBY', stockKey, quantity)";
    
    @Override
    public void warmUpStock(Long productId, Integer stock) {
        String key = STOCK_KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(key, stock);
        logger.info("库存预热完成：商品ID={}, 库存={}", productId, stock);
    }
    
    @Override
    public Long deductStock(Long productId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + productId;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DEDUCT_STOCK_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, Collections.singletonList(key), quantity.toString());
        
        if (result == null) {
            logger.error("库存扣减失败：商品ID={}, Lua脚本执行返回null", productId);
            return -2L;
        }
        
        if (result == -1) {
            logger.warn("库存不足：商品ID={}, 当前库存<请求数量", productId);
        } else if (result == -2) {
            logger.warn("库存不存在：商品ID={}, 请先进行库存预热", productId);
        } else {
            logger.info("库存扣减成功：商品ID={}, 扣减后库存={}", productId, result);
        }
        
        return result;
    }
    
    @Override
    public void rollbackStock(Long productId, Integer quantity) {
        String key = STOCK_KEY_PREFIX + productId;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(ROLLBACK_STOCK_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, Collections.singletonList(key), quantity.toString());
        logger.info("库存回滚完成：商品ID={}, 回滚数量={}, 当前库存={}", productId, quantity, result);
    }
    
    @Override
    public Integer getCurrentStock(Long productId) {
        String key = STOCK_KEY_PREFIX + productId;
        Object stock = redisTemplate.opsForValue().get(key);
        if (stock == null) {
            return null;
        }
        return Integer.parseInt(stock.toString());
    }
    
    @Override
    public void syncStockFromDB(Long productId) {
        var product = productMapper.selectById(productId);
        if (product != null) {
            warmUpStock(productId, product.getStock());
        }
    }
    
    @Override
    public boolean checkStockConsistency(Long productId) {
        Integer redisStock = getCurrentStock(productId);
        var product = productMapper.selectById(productId);
        
        if (product == null) {
            logger.warn("库存对账失败：商品不存在，ID={}", productId);
            return false;
        }
        
        if (redisStock == null) {
            logger.warn("库存对账失败：Redis库存不存在，商品ID={}", productId);
            return false;
        }
        
        boolean consistent = redisStock.equals(product.getStock());
        if (!consistent) {
            logger.warn("库存不一致：商品ID={}, Redis库存={}, DB库存={}", 
                    productId, redisStock, product.getStock());
        }
        
        return consistent;
    }
}
