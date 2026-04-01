package com.example.seckill_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀订单消息DTO
 * 用于Kafka消息传输
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long orderId;        // 订单ID
    private Long userId;         // 用户ID
    private Long productId;      // 商品ID
    private Integer quantity;    // 购买数量
    private BigDecimal price;    // 商品单价
    private Long timestamp;      // 消息时间戳
    private String traceId;      // 链路追踪ID
    
    // 重试次数
    private int retryCount = 0;
    
    // 最大重试次数
    public static final int MAX_RETRY_COUNT = 3;
}
