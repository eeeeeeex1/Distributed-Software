package com.example.seckill_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 秒杀响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResult {
    
    private int code;           // 状态码
    private String message;     // 消息
    private Long orderId;       // 订单ID
    private String orderNo;     // 订单编号
    private Long userId;        // 用户ID
    private Long productId;     // 商品ID
    private BigDecimal totalPrice; // 总价
    
    // 状态码常量
    public static final int SUCCESS = 200;
    public static final int STOCK_NOT_ENOUGH = 4001;
    public static final int ALREADY_SECKILLED = 4002;
    public static final int PRODUCT_NOT_EXIST = 4003;
    public static final int SYSTEM_ERROR = 5000;
    public static final int RATE_LIMITED = 429;
    public static final int INVALID_REQUEST = 400;
    
    public static SeckillResult success(Long orderId, String orderNo, Long userId, Long productId, BigDecimal totalPrice) {
        SeckillResult result = new SeckillResult();
        result.setCode(SUCCESS);
        result.setMessage("秒杀成功");
        result.setOrderId(orderId);
        result.setOrderNo(orderNo);
        result.setUserId(userId);
        result.setProductId(productId);
        result.setTotalPrice(totalPrice);
        return result;
    }
    
    public static SeckillResult stockNotEnough() {
        SeckillResult result = new SeckillResult();
        result.setCode(STOCK_NOT_ENOUGH);
        result.setMessage("库存不足");
        return result;
    }
    
    public static SeckillResult alreadySeckilled() {
        SeckillResult result = new SeckillResult();
        result.setCode(ALREADY_SECKILLED);
        result.setMessage("您已参与过此商品的秒杀");
        return result;
    }
    
    public static SeckillResult productNotExist() {
        SeckillResult result = new SeckillResult();
        result.setCode(PRODUCT_NOT_EXIST);
        result.setMessage("商品不存在");
        return result;
    }
    
    public static SeckillResult systemError(String msg) {
        SeckillResult result = new SeckillResult();
        result.setCode(SYSTEM_ERROR);
        result.setMessage("系统繁忙，请稍后重试: " + msg);
        return result;
    }
    
    public static SeckillResult rateLimited() {
        SeckillResult result = new SeckillResult();
        result.setCode(RATE_LIMITED);
        result.setMessage("请求过于频繁，请稍后重试");
        return result;
    }
    
    public static SeckillResult invalidRequest(String msg) {
        SeckillResult result = new SeckillResult();
        result.setCode(INVALID_REQUEST);
        result.setMessage(msg);
        return result;
    }
}
