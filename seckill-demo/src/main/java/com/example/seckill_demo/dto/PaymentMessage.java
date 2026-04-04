package com.example.seckill_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String transactionId;    // 事务ID
    private Long orderId;           // 订单ID
    private Long userId;            // 用户ID
    private BigDecimal amount;      // 支付金额
    private String paymentMethod;    // 支付方式
    private String paymentNo;       // 支付单号
    private Long timestamp;         // 消息时间戳
    private String traceId;         // 链路追踪ID
    
    // 重试次数
    private int retryCount = 0;
    
    // 最大重试次数
    public static final int MAX_RETRY_COUNT = 3;
}
