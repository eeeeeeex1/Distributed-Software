package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.dto.*;
import com.example.seckill_demo.service.StockService;
import com.example.seckill_demo.service.TransactionCoordinatorService;
import com.example.seckill_demo.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 事务协调服务实现类
 * 基于消息的最终一致性 + TCC事务
 */
@Service
public class TransactionCoordinatorServiceImpl implements TransactionCoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCoordinatorServiceImpl.class);
    
    // Redis Key前缀
    private static final String TRANSACTION_KEY_PREFIX = "transaction:seckill:";
    private static final String PAYMENT_TRANSACTION_KEY_PREFIX = "transaction:payment:";
    private static final String TRANSACTION_LOCK_PREFIX = "transaction:lock:";
    
    // 事务超时时间（毫秒）
    private static final long TRANSACTION_TIMEOUT = 10 * 60 * 1000; // 10分钟
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private StockService stockService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public TransactionResult beginSeckillTransaction(SeckillMessage message) {
        String transactionId = UUID.randomUUID().toString().replace("-", "");
        String key = TRANSACTION_KEY_PREFIX + transactionId;
        
        try {
            // 保存事务信息到Redis
            Map<String, Object> transactionInfo = new HashMap<>();
            transactionInfo.put("transactionId", transactionId);
            transactionInfo.put("orderId", message.getOrderId());
            transactionInfo.put("userId", message.getUserId());
            transactionInfo.put("productId", message.getProductId());
            transactionInfo.put("quantity", message.getQuantity());
            transactionInfo.put("status", TransactionStatus.INITIATED.name());
            transactionInfo.put("createTime", System.currentTimeMillis());
            transactionInfo.put("lastUpdateTime", System.currentTimeMillis());
            
            redisTemplate.opsForHash().putAll(key, transactionInfo);
            redisTemplate.expire(key, TRANSACTION_TIMEOUT, TimeUnit.MILLISECONDS);
            
            logger.info("秒杀事务开始：transactionId={}, orderId={}", transactionId, message.getOrderId());
            
            return TransactionResult.success(transactionId, TransactionStatus.INITIATED);
            
        } catch (Exception e) {
            logger.error("开始秒杀事务失败：message={}, error={}", message, e.getMessage(), e);
            return TransactionResult.failure("事务初始化失败");
        }
    }

    @Override
    public boolean commitSeckillTransaction(String transactionId) {
        String key = TRANSACTION_KEY_PREFIX + transactionId;
        
        try {
            // 检查事务是否存在
            if (!redisTemplate.hasKey(key)) {
                logger.warn("提交秒杀事务失败：事务不存在，transactionId={}", transactionId);
                return false;
            }
            
            // 更新事务状态为成功
            redisTemplate.opsForHash().put(key, "status", TransactionStatus.SUCCESS.name());
            redisTemplate.opsForHash().put(key, "lastUpdateTime", System.currentTimeMillis());
            
            // 延长过期时间
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            
            logger.info("秒杀事务提交成功：transactionId={}", transactionId);
            return true;
            
        } catch (Exception e) {
            logger.error("提交秒杀事务失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean rollbackSeckillTransaction(String transactionId) {
        String key = TRANSACTION_KEY_PREFIX + transactionId;
        
        try {
            // 检查事务是否存在
            if (!redisTemplate.hasKey(key)) {
                logger.warn("回滚秒杀事务失败：事务不存在，transactionId={}", transactionId);
                return false;
            }
            
            // 获取事务信息
            Long productId = Long.parseLong(redisTemplate.opsForHash().get(key, "productId").toString());
            Integer quantity = Integer.parseInt(redisTemplate.opsForHash().get(key, "quantity").toString());
            
            // 回滚库存
            stockService.rollbackStock(productId, quantity);
            
            // 更新事务状态为已回滚
            redisTemplate.opsForHash().put(key, "status", TransactionStatus.ROLLBACKED.name());
            redisTemplate.opsForHash().put(key, "lastUpdateTime", System.currentTimeMillis());
            
            logger.info("秒杀事务回滚成功：transactionId={}", transactionId);
            return true;
            
        } catch (Exception e) {
            logger.error("回滚秒杀事务失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public TransactionResult beginPaymentTransaction(PaymentMessage message) {
        String transactionId = message.getTransactionId();
        if (transactionId == null) {
            transactionId = UUID.randomUUID().toString().replace("-", "");
            message.setTransactionId(transactionId);
        }
        
        String key = PAYMENT_TRANSACTION_KEY_PREFIX + transactionId;
        
        try {
            // 保存支付事务信息到Redis
            Map<String, Object> transactionInfo = new HashMap<>();
            transactionInfo.put("transactionId", transactionId);
            transactionInfo.put("orderId", message.getOrderId());
            transactionInfo.put("userId", message.getUserId());
            transactionInfo.put("amount", message.getAmount());
            transactionInfo.put("paymentMethod", message.getPaymentMethod());
            transactionInfo.put("paymentNo", message.getPaymentNo());
            transactionInfo.put("status", TransactionStatus.INITIATED.name());
            transactionInfo.put("createTime", System.currentTimeMillis());
            transactionInfo.put("lastUpdateTime", System.currentTimeMillis());
            
            redisTemplate.opsForHash().putAll(key, transactionInfo);
            redisTemplate.expire(key, TRANSACTION_TIMEOUT, TimeUnit.MILLISECONDS);
            
            logger.info("支付事务开始：transactionId={}, orderId={}", transactionId, message.getOrderId());
            
            return TransactionResult.success(transactionId, TransactionStatus.INITIATED);
            
        } catch (Exception e) {
            logger.error("开始支付事务失败：message={}, error={}", message, e.getMessage(), e);
            return TransactionResult.failure("支付事务初始化失败");
        }
    }

    @Override
    public boolean commitPaymentTransaction(String transactionId) {
        String key = PAYMENT_TRANSACTION_KEY_PREFIX + transactionId;
        
        try {
            // 检查事务是否存在
            if (!redisTemplate.hasKey(key)) {
                logger.warn("提交支付事务失败：事务不存在，transactionId={}", transactionId);
                return false;
            }
            
            // 获取订单ID
            Long orderId = Long.parseLong(redisTemplate.opsForHash().get(key, "orderId").toString());
            
            // 更新订单状态为已支付
            boolean updated = orderService.updateOrderStatus(orderId, 1); // 1-已支付
            if (!updated) {
                logger.warn("更新订单状态失败：orderId={}", orderId);
                return false;
            }
            
            // 更新事务状态为成功
            redisTemplate.opsForHash().put(key, "status", TransactionStatus.SUCCESS.name());
            redisTemplate.opsForHash().put(key, "lastUpdateTime", System.currentTimeMillis());
            
            // 延长过期时间
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            
            logger.info("支付事务提交成功：transactionId={}, orderId={}", transactionId, orderId);
            return true;
            
        } catch (Exception e) {
            logger.error("提交支付事务失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean rollbackPaymentTransaction(String transactionId) {
        String key = PAYMENT_TRANSACTION_KEY_PREFIX + transactionId;
        
        try {
            // 检查事务是否存在
            if (!redisTemplate.hasKey(key)) {
                logger.warn("回滚支付事务失败：事务不存在，transactionId={}", transactionId);
                return false;
            }
            
            // 更新事务状态为已回滚
            redisTemplate.opsForHash().put(key, "status", TransactionStatus.ROLLBACKED.name());
            redisTemplate.opsForHash().put(key, "lastUpdateTime", System.currentTimeMillis());
            
            logger.info("支付事务回滚成功：transactionId={}", transactionId);
            return true;
            
        } catch (Exception e) {
            logger.error("回滚支付事务失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void handleTimeoutTransactions() {
        try {
            // 处理秒杀超时事务
            Set<String> seckillTransactions = redisTemplate.keys(TRANSACTION_KEY_PREFIX + "*");
            if (seckillTransactions != null) {
                for (String key : seckillTransactions) {
                    String transactionId = key.substring(TRANSACTION_KEY_PREFIX.length());
                    handleTimeoutTransaction(key, transactionId, true);
                }
            }
            
            // 处理支付超时事务
            Set<String> paymentTransactions = redisTemplate.keys(PAYMENT_TRANSACTION_KEY_PREFIX + "*");
            if (paymentTransactions != null) {
                for (String key : paymentTransactions) {
                    String transactionId = key.substring(PAYMENT_TRANSACTION_KEY_PREFIX.length());
                    handleTimeoutTransaction(key, transactionId, false);
                }
            }
            
        } catch (Exception e) {
            logger.error("处理超时事务异常：error={}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理超时事务
     */
    private void handleTimeoutTransaction(String key, String transactionId, boolean isSeckill) {
        try {
            Object statusObj = redisTemplate.opsForHash().get(key, "status");
            Object createTimeObj = redisTemplate.opsForHash().get(key, "createTime");
            
            if (statusObj == null || createTimeObj == null) {
                return;
            }
            
            String status = statusObj.toString();
            long createTime = Long.parseLong(createTimeObj.toString());
            long currentTime = System.currentTimeMillis();
            
            // 检查是否超时
            if (currentTime - createTime > TRANSACTION_TIMEOUT) {
                if (!TransactionStatus.SUCCESS.name().equals(status) && 
                    !TransactionStatus.ROLLBACKED.name().equals(status)) {
                    
                    logger.warn("事务超时：transactionId={}, status={}", transactionId, status);
                    
                    // 回滚事务
                    if (isSeckill) {
                        rollbackSeckillTransaction(transactionId);
                    } else {
                        rollbackPaymentTransaction(transactionId);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("处理超时事务失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
        }
    }

    @Override
    public TransactionStatus getTransactionStatus(String transactionId) {
        String seckillKey = TRANSACTION_KEY_PREFIX + transactionId;
        String paymentKey = PAYMENT_TRANSACTION_KEY_PREFIX + transactionId;
        
        try {
            if (redisTemplate.hasKey(seckillKey)) {
                Object statusObj = redisTemplate.opsForHash().get(seckillKey, "status");
                if (statusObj != null) {
                    return TransactionStatus.valueOf(statusObj.toString());
                }
            }
            
            if (redisTemplate.hasKey(paymentKey)) {
                Object statusObj = redisTemplate.opsForHash().get(paymentKey, "status");
                if (statusObj != null) {
                    return TransactionStatus.valueOf(statusObj.toString());
                }
            }
            
        } catch (Exception e) {
            logger.error("获取事务状态失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
        }
        
        return null;
    }
}
