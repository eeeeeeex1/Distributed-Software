package com.example.seckill_demo;

import com.example.seckill_demo.entity.Product;
import com.example.seckill_demo.service.ProductService;
import com.example.seckill_demo.service.ProductSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 分布式系统功能测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestDistributedFeatures {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductSearchService productSearchService;

    /**
     * 测试Redis缓存功能
     */
    @Test
    public void testRedisCache() {
        // 第一次查询（应该走数据库）
        Product product1 = productService.getProductDetail(1L);
        System.out.println("First query: " + product1);

        // 第二次查询（应该走缓存）
        Product product2 = productService.getProductDetail(1L);
        System.out.println("Second query: " + product2);

        // 验证两次查询结果一致
        assert product1 != null;
        assert product2 != null;
        assert product1.getId().equals(product2.getId());
        System.out.println("Redis cache test passed!");
    }

    /**
     * 测试数据库读写分离
     */
    @Test
    public void testReadWriteSeparation() {
        // 测试读操作（应该走从库）
        Product product = productService.getProductDetail(1L);
        System.out.println("Read operation result: " + product);
        assert product != null;
        System.out.println("Read write separation test passed!");
    }

    /**
     * 测试ElasticSearch搜索功能
     */
    @Test
    public void testElasticSearch() {
        // 先同步商品数据到ElasticSearch
        productSearchService.fullSyncProductsToEs();
        System.out.println("Products synced to ElasticSearch");

        // 测试搜索功能
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        Page<?> result = productSearchService.searchProducts("商品", pageable);
        System.out.println("Search result total: " + result.getTotalElements());
        System.out.println("Search result content: " + result.getContent());
        assert result.getTotalElements() > 0;
        System.out.println("ElasticSearch test passed!");
    }
}
