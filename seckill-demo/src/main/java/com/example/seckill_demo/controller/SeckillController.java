package com.example.seckill_demo.controller;

import com.example.seckill_demo.dto.SeckillResult;
import com.example.seckill_demo.entity.Order;
import com.example.seckill_demo.service.OrderService;
import com.example.seckill_demo.service.SeckillService;
import com.example.seckill_demo.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀控制器
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private StockService stockService;

    /**
     * 执行秒杀
     * POST /api/seckill/do
     * Body: {"userId": 1, "productId": 1, "quantity": 1}
     */
    @PostMapping("/do")
    public Map<String, Object> doSeckill(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long userId = Long.parseLong(params.get("userId").toString());
            Long productId = Long.parseLong(params.get("productId").toString());
            Integer quantity = params.containsKey("quantity") ? 
                    Integer.parseInt(params.get("quantity").toString()) : 1;
            
            SeckillResult seckillResult = seckillService.doSeckill(userId, productId, quantity);
            
            result.put("code", seckillResult.getCode());
            result.put("message", seckillResult.getMessage());
            if (seckillResult.getOrderId() != null) {
                result.put("data", Map.of(
                        "orderId", seckillResult.getOrderId(),
                        "orderNo", seckillResult.getOrderNo(),
                        "userId", seckillResult.getUserId(),
                        "productId", seckillResult.getProductId(),
                        "totalPrice", seckillResult.getTotalPrice()
                ));
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "系统异常：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 查询订单（按订单ID）
     * GET /api/seckill/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public Map<String, Object> getOrderById(@PathVariable Long orderId) {
        Map<String, Object> result = new HashMap<>();
        
        Order order = orderService.getOrderById(orderId);
        if (order != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", order);
        } else {
            result.put("code", 404);
            result.put("message", "订单不存在");
        }
        
        return result;
    }
    
    /**
     * 查询订单（按订单编号）
     * GET /api/seckill/order/no/{orderNo}
     */
    @GetMapping("/order/no/{orderNo}")
    public Map<String, Object> getOrderByNo(@PathVariable String orderNo) {
        Map<String, Object> result = new HashMap<>();
        
        Order order = orderService.getOrderByOrderNo(orderNo);
        if (order != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", order);
        } else {
            result.put("code", 404);
            result.put("message", "订单不存在");
        }
        
        return result;
    }
    
    /**
     * 查询用户订单列表
     * GET /api/seckill/orders/{userId}
     */
    @GetMapping("/orders/{userId}")
    public Map<String, Object> getUserOrders(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        List<Order> orders = orderService.getOrdersByUserId(userId);
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", orders);
        
        return result;
    }
    
    /**
     * 库存预热
     * POST /api/seckill/stock/warmup
     */
    @PostMapping("/stock/warmup")
    public Map<String, Object> warmUpStock() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            seckillService.warmUpAllStock();
            result.put("code", 200);
            result.put("message", "库存预热完成");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "库存预热失败：" + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 查询当前库存
     * GET /api/seckill/stock/{productId}
     */
    @GetMapping("/stock/{productId}")
    public Map<String, Object> getStock(@PathVariable Long productId) {
        Map<String, Object> result = new HashMap<>();
        
        Integer stock = stockService.getCurrentStock(productId);
        if (stock != null) {
            result.put("code", 200);
            result.put("message", "success");
            result.put("data", Map.of("productId", productId, "stock", stock));
        } else {
            result.put("code", 404);
            result.put("message", "库存信息不存在，请先预热");
        }
        
        return result;
    }
    
    /**
     * 库存对账
     * GET /api/seckill/stock/check/{productId}
     */
    @GetMapping("/stock/check/{productId}")
    public Map<String, Object> checkStock(@PathVariable Long productId) {
        Map<String, Object> result = new HashMap<>();
        
        boolean consistent = stockService.checkStockConsistency(productId);
        result.put("code", 200);
        result.put("message", consistent ? "库存一致" : "库存不一致，请检查");
        result.put("data", Map.of("consistent", consistent));
        
        return result;
    }
}
