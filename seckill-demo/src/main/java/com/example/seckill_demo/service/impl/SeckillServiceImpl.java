package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.dto.SeckillMessage;
import com.example.seckill_demo.dto.SeckillResult;
import com.example.seckill_demo.dto.TransactionResult;
import com.example.seckill_demo.entity.Order;
import com.example.seckill_demo.entity.Product;
import com.example.seckill_demo.mapper.ProductMapper;
import com.example.seckill_demo.service.ProductService;
import com.example.seckill_demo.service.SeckillService;
import com.example.seckill_demo.service.StockService;
import com.example.seckill_demo.service.TransactionCoordinatorService;
import com.example.seckill_demo.util.SnowflakeIdGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现类
 * 核心流程：幂等校验 -> 库存预扣减 -> 发送Kafka消息 -> 异步创建订单
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    private static final Logger logger = LoggerFactory.getLogger(SeckillServiceImpl.class);
    
    // Redis Key前缀
    private static final String SECKILL_USER_KEY = "seckill:user:";      // 用户秒杀记录
    private static final String SECKILL_LOCK_KEY = "seckill:lock:";      // 秒杀分布式锁
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private SnowflakeIdGenerator idGenerator;
    
    @Autowired
    private OrderServiceImpl orderService;
    
    @Autowired
    private TransactionCoordinatorService transactionCoordinatorService;

    @Override
    @RateLimiter(name = "seckillRateLimiter", fallbackMethod = "doSeckillRateLimitFallback")
    @CircuitBreaker(name = "seckillCircuitBreaker", fallbackMethod = "doSeckillFallback")
    public SeckillResult doSeckill(Long userId, Long productId, Integer quantity) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        MDC.put("traceId", traceId);
        
        try {
            logger.info("秒杀请求开始：userId={}, productId={}, quantity={}", userId, productId, quantity);
            
            // 1. 参数校验
            if (userId == null || userId <= 0 || productId == null || productId <= 0) {
                return SeckillResult.invalidRequest("参数错误");
            }
            if (quantity == null || quantity <= 0) {
                quantity = 1;
            }
            
            // 2. 幂等性校验：检查用户是否已秒杀过
            if (!checkSeckillQualification(userId, productId)) {
                logger.warn("重复秒杀：userId={}, productId={}", userId, productId);
                return SeckillResult.alreadySeckilled();
            }
            
            // 3. 获取商品信息（带缓存）
            Product product = productService.getProductDetail(productId);
            if (product == null) {
                logger.warn("商品不存在：productId={}", productId);
                return SeckillResult.productNotExist();
            }
            
            // 4. 库存预扣减（Redis原子操作）
            Long remainStock = stockService.deductStock(productId, quantity);
            if (remainStock < 0) {
                logger.warn("库存不足：productId={}, remainStock={}", productId, remainStock);
                return SeckillResult.stockNotEnough();
            }
            
            // 5. 生成订单ID
            Long orderId = idGenerator.nextId();
            String orderNo = generateOrderNo(orderId);
            
            // 6. 标记用户已秒杀
            markUserSeckilled(userId, productId);
            
            // 7. 开始秒杀事务
            SeckillMessage message = new SeckillMessage();
            message.setOrderId(orderId);
            message.setUserId(userId);
            message.setProductId(productId);
            message.setQuantity(quantity);
            message.setPrice(product.getPrice());
            message.setTimestamp(System.currentTimeMillis());
            message.setTraceId(traceId);
            
            // 开始事务协调
            TransactionResult transactionResult = transactionCoordinatorService.beginSeckillTransaction(message);
            if (!transactionResult.isSuccess()) {
                logger.error("开始秒杀事务失败：userId={}, productId={}", userId, productId);
                // 回滚库存
                stockService.rollbackStock(productId, quantity);
                return SeckillResult.systemError(transactionResult.getMessage());
            }
            
            // 8. 发送Kafka消息（异步处理订单）
            sendSeckillMessage(message);
            
            // 8. 返回秒杀结果（订单异步处理中）
            BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            logger.info("秒杀请求成功：orderId={}, userId={}, productId={}", orderId, userId, productId);
            
            return SeckillResult.success(orderId, orderNo, userId, productId, totalPrice);
            
        } catch (Exception e) {
            logger.error("秒杀异常：userId={}, productId={}, error={}", userId, productId, e.getMessage(), e);
            // 异常时回滚库存
            stockService.rollbackStock(productId, quantity);
            return SeckillResult.systemError(e.getMessage());
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 发送秒杀消息到Kafka
     */
    private void sendSeckillMessage(SeckillMessage message) {
        String key = String.valueOf(message.getUserId());
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(
                "seckill-order", 
                key, 
                message
        );
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Kafka消息发送失败：orderId={}, error={}", 
                        message.getOrderId(), ex.getMessage());
                // 消息发送失败，回滚库存和用户标记
                stockService.rollbackStock(message.getProductId(), message.getQuantity());
                removeUserSeckillMark(message.getUserId(), message.getProductId());
            } else {
                logger.info("Kafka消息发送成功：orderId={}, partition={}, offset={}", 
                        message.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    @Override
    public void processSeckillOrder(SeckillMessage message) {
        MDC.put("traceId", message.getTraceId());
        
        try {
            logger.info("开始处理秒杀订单：orderId={}, userId={}, productId={}", 
                    message.getOrderId(), message.getUserId(), message.getProductId());
            
            // 创建订单
            Order order = new Order();
            order.setId(message.getOrderId());
            order.setUserId(message.getUserId());
            order.setProductId(message.getProductId());
            order.setQuantity(message.getQuantity());
            order.setTotalPrice(message.getPrice().multiply(BigDecimal.valueOf(message.getQuantity())));
            order.setStatus(Order.STATUS_PENDING);
            order.setOrderNo(generateOrderNo(message.getOrderId()));
            
            // 保存订单到数据库
            orderService.createOrder(order);
            
            // 扣减数据库库存
            int updated = productMapper.deductStock(message.getProductId(), message.getQuantity());
            if (updated <= 0) {
                logger.error("数据库库存扣减失败：productId={}", message.getProductId());
                throw new RuntimeException("数据库库存扣减失败");
            }
            
            // 提交事务
            transactionCoordinatorService.commitSeckillTransaction(message.getOrderId().toString());
            logger.info("秒杀订单处理完成：orderId={}", message.getOrderId());
            
        } catch (Exception e) {
            logger.error("秒杀订单处理失败：orderId={}, error={}", 
                    message.getOrderId(), e.getMessage(), e);
            
            // 重试逻辑
            if (message.getRetryCount() < SeckillMessage.MAX_RETRY_COUNT) {
                message.setRetryCount(message.getRetryCount() + 1);
                logger.info("重试处理订单：orderId={}, retryCount={}", 
                        message.getOrderId(), message.getRetryCount());
                kafkaTemplate.send("seckill-order", String.valueOf(message.getUserId()), message);
            } else {
                logger.error("订单处理达到最大重试次数，转入死信队列：orderId={}", message.getOrderId());
                kafkaTemplate.send("seckill-order-dlt", String.valueOf(message.getUserId()), message);
                // 回滚Redis库存和事务
                stockService.rollbackStock(message.getProductId(), message.getQuantity());
                removeUserSeckillMark(message.getUserId(), message.getProductId());
                transactionCoordinatorService.rollbackSeckillTransaction(message.getOrderId().toString());
            }
        } finally {
            MDC.clear();
        }
    }

    @Override
    public boolean checkSeckillQualification(Long userId, Long productId) {
        String key = SECKILL_USER_KEY + userId + ":" + productId;
        Object exists = redisTemplate.opsForValue().get(key);
        return exists == null;
    }

    @Override
    public void markUserSeckilled(Long userId, Long productId) {
        String key = SECKILL_USER_KEY + userId + ":" + productId;
        // 设置过期时间（如24小时）
        redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);
        logger.info("标记用户已秒杀：userId={}, productId={}", userId, productId);
    }
    
    /**
     * 移除用户秒杀标记（用于回滚）
     */
    private void removeUserSeckillMark(Long userId, Long productId) {
        String key = SECKILL_USER_KEY + userId + ":" + productId;
        redisTemplate.delete(key);
        logger.info("移除用户秒杀标记：userId={}, productId={}", userId, productId);
    }

    @Override
    public void warmUpAllStock() {
        logger.info("开始库存预热...");
        var products = productMapper.selectList();
        for (Product product : products) {
            stockService.warmUpStock(product.getId(), product.getStock());
        }
        logger.info("库存预热完成，共{}个商品", products.size());
    }
    
    /**
     * 生成订单编号
     */
    private String generateOrderNo(Long orderId) {
        return "SK" + orderId;
    }
    
    /**
     * 限流降级方法
     */
    public SeckillResult doSeckillRateLimitFallback(Long userId, Long productId, Integer quantity, Throwable t) {
        logger.warn("秒杀限流触发：userId={}, productId={}", userId, productId);
        return SeckillResult.rateLimited();
    }
    
    /**
     * 熔断降级方法
     */
    public SeckillResult doSeckillFallback(Long userId, Long productId, Integer quantity, Throwable t) {
        logger.error("秒杀熔断触发：userId={}, productId={}, error={}", userId, productId, t.getMessage());
        return SeckillResult.systemError("服务繁忙，请稍后重试");
    }
}
