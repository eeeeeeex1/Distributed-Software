package com.example.seckill_demo.service;

import com.example.seckill_demo.entity.Order;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {
    
    /**
     * 创建订单
     */
    void createOrder(Order order);
    
    /**
     * 根据订单ID查询订单
     */
    Order getOrderById(Long orderId);
    
    /**
     * 根据用户ID查询订单列表
     */
    List<Order> getOrdersByUserId(Long userId);
    
    /**
     * 更新订单状态
     */
    boolean updateOrderStatus(Long orderId, Integer status);
    
    /**
     * 根据订单编号查询订单
     */
    Order getOrderByOrderNo(String orderNo);
}
