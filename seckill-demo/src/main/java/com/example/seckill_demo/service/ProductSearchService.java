package com.example.seckill_demo.service;

import com.example.seckill_demo.entity.Product;
import com.example.seckill_demo.entity.ProductDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 商品搜索服务
 */
public interface ProductSearchService {

    /**
     * 同步单个商品到ElasticSearch
     */
    void syncProductToEs(Product product);

    /**
     * 批量同步商品到ElasticSearch
     */
    void syncProductsToEs(List<Product> products);

    /**
     * 从ElasticSearch删除商品
     */
    void deleteProductFromEs(Long productId);

    /**
     * 搜索商品
     */
    Page<ProductDocument> searchProducts(String keyword, Pageable pageable);

    /**
     * 全量同步所有商品到ElasticSearch
     */
    void fullSyncProductsToEs();
}
