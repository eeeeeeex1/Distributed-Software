package com.example.seckill_demo.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品实体类（适配秒杀场景）
 */
@Data
public class Product implements Serializable {
    private Long id;             // 商品ID
    private String name;         // 商品名称
    private BigDecimal price;    // 商品价格
    private Integer stock;       // 商品库存
    private String desc;         // 商品描述
    private Date createTime;     // 创建时间
    private Date updateTime;     // 更新时间
}