package com.example.seckill_demo.controller;

import com.example.seckill_demo.entity.ProductDocument;
import com.example.seckill_demo.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品搜索Controller
 * 仅在elasticsearch.enabled=true时启用
 */
@RestController
@RequestMapping("/api/search")
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
public class ProductSearchController {

    @Autowired
    private ProductSearchService productSearchService;

    /**
     * 搜索商品
     */
    @GetMapping("/products")
    public Map<String, Object> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        // 构建分页和排序
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // 执行搜索
        Page<ProductDocument> result = productSearchService.searchProducts(keyword, pageable);

        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("total", result.getTotalElements());
        response.put("pages", result.getTotalPages());
        response.put("current", page);
        response.put("size", size);
        response.put("products", result.getContent());

        return response;
    }

    /**
     * 同步商品到ElasticSearch
     */
    @PostMapping("/sync")
    public Map<String, Object> syncProducts() {
        productSearchService.fullSyncProductsToEs();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "商品同步成功");
        return response;
    }
}
