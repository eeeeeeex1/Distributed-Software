package com.example.seckill_demo.controller;

import com.example.seckill_demo.entity.Product;
import com.example.seckill_demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品控制器（高并发读核心接口）
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 获取商品详情（带Redis缓存）
     */
    @GetMapping("/detail/{id}")
    public Map<String, Object> getProductDetail(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            Product product = productService.getProductDetail(id);
            if (product != null) {
                result.put("code", 200);
                result.put("msg", "success");
                result.put("data", product);
            } else {
                result.put("code", 404);
                result.put("msg", "商品不存在");
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "服务器异常：" + e.getMessage());
        }
        return result;
    }
}