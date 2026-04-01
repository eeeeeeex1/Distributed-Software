package com.example.seckill_demo.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单实体类
 */
@Data
public class Order {
    private Long id;              // 订单ID（雪花算法生成）
    private Long userId;          // 用户ID
    private Long productId;       // 商品ID
    private Integer quantity;     // 购买数量
    private BigDecimal totalPrice;// 总价
    private Integer status;       // 订单状态：0-待支付，1-已支付，2-已取消，3-已退款
    private String orderNo;       // 订单编号
    private Date createTime;      // 创建时间
    private Date updateTime;      // 更新时间
    
    // 订单状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_CANCELLED = 2;
    public static final int STATUS_REFUNDED = 3;
}
