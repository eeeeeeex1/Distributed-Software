package com.example.seckill_demo.entity;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品ElasticSearch文档实体
 */
@Data
@Document(indexName = "products")
public class ProductDocument {

    @Field(type = FieldType.Long)
    private Long id;             // 商品ID

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;         // 商品名称

    @Field(type = FieldType.Double)
    private BigDecimal price;    // 商品价格

    @Field(type = FieldType.Integer)
    private Integer stock;       // 商品库存

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String desc;         // 商品描述

    @Field(type = FieldType.Date)
    private Date createTime;     // 创建时间

    @Field(type = FieldType.Date)
    private Date updateTime;     // 更新时间
}
