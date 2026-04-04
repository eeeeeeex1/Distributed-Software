package com.example.seckill_demo.service;

import com.example.seckill_demo.dto.PaymentMessage;
import com.example.seckill_demo.dto.TransactionResult;

/**
 * 支付服务接口
 * 实现TCC事务模式：Try-Confirm-Cancel
 */
public interface PaymentService {
    
    /**
     * Try阶段：检查和预留支付资源
     */
    TransactionResult tryPayment(PaymentMessage message);
    
    /**
     * Confirm阶段：确认支付
     */
    boolean confirmPayment(String transactionId);
    
    /**
     * Cancel阶段：取消支付
     */
    boolean cancelPayment(String transactionId);
    
    /**
     * 处理支付回调
     */
    boolean handlePaymentCallback(String transactionId, String paymentStatus, String paymentNo);
    
    /**
     * 发起支付
     */
    TransactionResult initiatePayment(Long orderId, Long userId, String paymentMethod);
    
    /**
     * 检查支付状态
     */
    String checkPaymentStatus(String transactionId);
}
