package com.example.seckill_demo.consumer;

import com.example.seckill_demo.dto.PaymentMessage;
import com.example.seckill_demo.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 支付消息消费者
 */
@Component
public class PaymentMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentMessageConsumer.class);
    
    @Autowired
    private PaymentService paymentService;

    @KafkaListener(
            topics = "payment-topic",
            groupId = "payment-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentMessage(PaymentMessage message) {
        MDC.put("traceId", message.getTraceId());
        
        try {
            logger.info("收到支付消息：transactionId={}, orderId={}", 
                    message.getTransactionId(), message.getOrderId());
            
            // 模拟支付处理
            processPayment(message);
            
        } catch (Exception e) {
            logger.error("处理支付消息失败：message={}, error={}", message, e.getMessage(), e);
            
            // 重试逻辑
            if (message.getRetryCount() < PaymentMessage.MAX_RETRY_COUNT) {
                message.setRetryCount(message.getRetryCount() + 1);
                logger.info("重试处理支付消息：transactionId={}, retryCount={}", 
                        message.getTransactionId(), message.getRetryCount());
                // 这里可以重新发送到Kafka
            } else {
                logger.error("支付消息处理达到最大重试次数：transactionId={}", message.getTransactionId());
                // 执行Cancel
                paymentService.cancelPayment(message.getTransactionId());
            }
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 处理支付
     */
    private void processPayment(PaymentMessage message) {
        try {
            // 模拟支付处理延迟
            Thread.sleep(1000);
            
            // 模拟支付成功
            boolean success = true;
            
            if (success) {
                // 支付成功，执行Confirm
                paymentService.confirmPayment(message.getTransactionId());
                logger.info("支付处理成功：transactionId={}, orderId={}", 
                        message.getTransactionId(), message.getOrderId());
            } else {
                // 支付失败，执行Cancel
                paymentService.cancelPayment(message.getTransactionId());
                logger.warn("支付处理失败：transactionId={}, orderId={}", 
                        message.getTransactionId(), message.getOrderId());
            }
            
        } catch (Exception e) {
            logger.error("处理支付失败：message={}, error={}", message, e.getMessage(), e);
            throw new RuntimeException("支付处理失败", e);
        }
    }
}
