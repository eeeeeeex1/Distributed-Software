package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.entity.Product;
import com.example.seckill_demo.mapper.ProductMapper;
import com.example.seckill_demo.service.ReconciliationService;
import com.example.seckill_demo.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对账服务实现类
 * 定期检查Redis和数据库数据一致性
 */
@Service
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final Logger logger = LoggerFactory.getLogger(ReconciliationServiceImpl.class);
    
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private StockService stockService;

    @Override
    public Map<String, Object> reconcileStock() {
        Map<String, Object> result = new HashMap<>();
        int consistent = 0;
        int inconsistent = 0;
        int missing = 0;
        
        List<Product> products = productMapper.selectList();
        
        for (Product product : products) {
            String key = STOCK_KEY_PREFIX + product.getId();
            Object redisStock = redisTemplate.opsForValue().get(key);
            
            if (redisStock == null) {
                missing++;
                logger.warn("库存对账：Redis库存缺失，productId={}", product.getId());
            } else {
                int redisStockInt = Integer.parseInt(redisStock.toString());
                if (redisStockInt != product.getStock()) {
                    inconsistent++;
                    logger.warn("库存对账：库存不一致，productId={}, Redis={}, DB={}", 
                            product.getId(), redisStockInt, product.getStock());
                } else {
                    consistent++;
                }
            }
        }
        
        result.put("total", products.size());
        result.put("consistent", consistent);
        result.put("inconsistent", inconsistent);
        result.put("missing", missing);
        
        logger.info("库存对账完成：{}", result);
        return result;
    }

    @Override
    public Map<String, Object> reconcileOrders() {
        Map<String, Object> result = new HashMap<>();
        
        // TODO: 实现订单对账逻辑
        // 检查Redis中的订单标记与数据库订单的一致性
        
        result.put("message", "订单对账功能待实现");
        return result;
    }

    @Override
    public void fixStockInconsistency(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product != null) {
            stockService.warmUpStock(productId, product.getStock());
            logger.info("修复库存不一致：productId={}, stock={}", productId, product.getStock());
        }
    }

    @Override
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void scheduledReconciliation() {
        logger.info("开始定时对账任务...");
        
        try {
            Map<String, Object> stockResult = reconcileStock();
            
            // 自动修复缺失的库存
            if ((int) stockResult.get("missing") > 0) {
                List<Product> products = productMapper.selectList();
                for (Product product : products) {
                    String key = STOCK_KEY_PREFIX + product.getId();
                    if (redisTemplate.opsForValue().get(key) == null) {
                        stockService.warmUpStock(product.getId(), product.getStock());
                    }
                }
            }
            
            logger.info("定时对账任务完成：{}", stockResult);
        } catch (Exception e) {
            logger.error("定时对账任务异常：{}", e.getMessage(), e);
        }
    }
}
