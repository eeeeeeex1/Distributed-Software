package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.dto.PaymentMessage;
import com.example.seckill_demo.dto.TransactionResult;
import com.example.seckill_demo.entity.Order;
import com.example.seckill_demo.service.OrderService;
import com.example.seckill_demo.service.PaymentService;
import com.example.seckill_demo.service.TransactionCoordinatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现类
 * 基于TCC事务模式保障订单支付一致性
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    
    // Redis Key前缀
    private static final String PAYMENT_LOCK_PREFIX = "payment:lock:";
    private static final String PAYMENT_RESERVE_PREFIX = "payment:reserve:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TransactionCoordinatorService transactionCoordinatorService;
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public TransactionResult tryPayment(PaymentMessage message) {
        String traceId = message.getTraceId();
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            message.setTraceId(traceId);
        }
        MDC.put("traceId", traceId);
        
        try {
            logger.info("开始Try支付：orderId={}, userId={}, amount={}", 
                    message.getOrderId(), message.getUserId(), message.getAmount());
            
            // 1. 检查订单状态
            Order order = orderService.getOrderById(message.getOrderId());
            if (order == null) {
                logger.warn("订单不存在：orderId={}", message.getOrderId());
                return TransactionResult.failure("订单不存在");
            }
            
            if (order.getStatus() != 0) { // 0-待支付
                logger.warn("订单状态异常：orderId={}, status={}", message.getOrderId(), order.getStatus());
                return TransactionResult.failure("订单状态异常");
            }
            
            // 2. 检查金额一致性
            if (!order.getTotalPrice().equals(message.getAmount())) {
                logger.warn("支付金额不一致：orderId={}, orderAmount={}, paymentAmount={}", 
                        message.getOrderId(), order.getTotalPrice(), message.getAmount());
                return TransactionResult.failure("支付金额不一致");
            }
            
            // 3. 预留支付资源（在Redis中标记）
            String reserveKey = PAYMENT_RESERVE_PREFIX + message.getTransactionId();
            redisTemplate.opsForValue().set(reserveKey, "1", 30, TimeUnit.MINUTES);
            
            // 4. 开始支付事务
            TransactionResult result = transactionCoordinatorService.beginPaymentTransaction(message);
            if (!result.isSuccess()) {
                logger.error("开始支付事务失败：message={}", message);
                return result;
            }
            
            // 5. 发送支付消息到Kafka
            sendPaymentMessage(message);
            
            logger.info("Try支付成功：transactionId={}, orderId={}", message.getTransactionId(), message.getOrderId());
            return result;
            
        } catch (Exception e) {
            logger.error("Try支付失败：message={}, error={}", message, e.getMessage(), e);
            return TransactionResult.failure("支付处理失败");
        } finally {
            MDC.clear();
        }
    }

    @Override
    public boolean confirmPayment(String transactionId) {
        try {
            logger.info("开始Confirm支付：transactionId={}", transactionId);
            
            // 提交支付事务
            boolean success = transactionCoordinatorService.commitPaymentTransaction(transactionId);
            
            // 清除预留资源
            String reserveKey = PAYMENT_RESERVE_PREFIX + transactionId;
            redisTemplate.delete(reserveKey);
            
            logger.info("Confirm支付{}", success ? "成功" : "失败");
            return success;
            
        } catch (Exception e) {
            logger.error("Confirm支付失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean cancelPayment(String transactionId) {
        try {
            logger.info("开始Cancel支付：transactionId={}", transactionId);
            
            // 回滚支付事务
            boolean success = transactionCoordinatorService.rollbackPaymentTransaction(transactionId);
            
            // 清除预留资源
            String reserveKey = PAYMENT_RESERVE_PREFIX + transactionId;
            redisTemplate.delete(reserveKey);
            
            logger.info("Cancel支付{}", success ? "成功" : "失败");
            return success;
            
        } catch (Exception e) {
            logger.error("Cancel支付失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean handlePaymentCallback(String transactionId, String paymentStatus, String paymentNo) {
        try {
            logger.info("处理支付回调：transactionId={}, status={}, paymentNo={}", 
                    transactionId, paymentStatus, paymentNo);
            
            if ("SUCCESS".equals(paymentStatus)) {
                // 支付成功，执行Confirm
                return confirmPayment(transactionId);
            } else {
                // 支付失败，执行Cancel
                return cancelPayment(transactionId);
            }
            
        } catch (Exception e) {
            logger.error("处理支付回调失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public TransactionResult initiatePayment(Long orderId, Long userId, String paymentMethod) {
        try {
            // 获取订单信息
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return TransactionResult.failure("订单不存在");
            }
            
            if (!order.getUserId().equals(userId)) {
                return TransactionResult.failure("无权操作此订单");
            }
            
            // 创建支付消息
            PaymentMessage message = new PaymentMessage();
            message.setTransactionId(UUID.randomUUID().toString().replace("-", ""));
            message.setOrderId(orderId);
            message.setUserId(userId);
            message.setAmount(order.getTotalPrice());
            message.setPaymentMethod(paymentMethod);
            message.setTimestamp(System.currentTimeMillis());
            
            // 执行Try阶段
            return tryPayment(message);
            
        } catch (Exception e) {
            logger.error("发起支付失败：orderId={}, userId={}, error={}", orderId, userId, e.getMessage(), e);
            return TransactionResult.failure("发起支付失败");
        }
    }

    @Override
    public String checkPaymentStatus(String transactionId) {
        try {
            // 检查事务状态
            var status = transactionCoordinatorService.getTransactionStatus(transactionId);
            if (status != null) {
                return status.name();
            }
            return "UNKNOWN";
            
        } catch (Exception e) {
            logger.error("检查支付状态失败：transactionId={}, error={}", transactionId, e.getMessage(), e);
            return "ERROR";
        }
    }
    
    /**
     * 发送支付消息到Kafka
     */
    private void sendPaymentMessage(PaymentMessage message) {
        String key = String.valueOf(message.getUserId());
        
        CompletableFuture future = kafkaTemplate.send("payment-topic", key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Kafka支付消息发送失败：transactionId={}, error={}", 
                        message.getTransactionId(), ex.toString());
                // 消息发送失败，执行Cancel
                cancelPayment(message.getTransactionId());
            } else {
                logger.info("Kafka支付消息发送成功：transactionId={}", 
                        message.getTransactionId());
            }
        });
    }
}
