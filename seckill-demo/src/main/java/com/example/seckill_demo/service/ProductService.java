package com.example.seckill_demo.service;

import com.example.seckill_demo.entity.Product;

/**
 * 商品服务接口
 */
public interface ProductService {
    /**
     * 获取商品详情（带Redis缓存）
     */
    Product getProductDetail(Long productId);
}