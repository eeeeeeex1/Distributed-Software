package com.example.seckill_demo.service.impl;

import com.example.seckill_demo.entity.Order;
import com.example.seckill_demo.mapper.OrderMapper;
import com.example.seckill_demo.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现类
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);
    
    private static final String ORDER_CACHE_KEY = "order:";
    private static final String USER_ORDERS_KEY = "user:orders:";
    private static final long CACHE_TTL = 30; // 30分钟
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        orderMapper.insertOrder(order);
        
        // 缓存订单
        String key = ORDER_CACHE_KEY + order.getId();
        redisTemplate.opsForValue().set(key, order, CACHE_TTL, TimeUnit.MINUTES);
        
        // 清除用户订单列表缓存
        String userOrdersKey = USER_ORDERS_KEY + order.getUserId();
        redisTemplate.delete(userOrdersKey);
        
        logger.info("订单创建成功：orderId={}, userId={}", order.getId(), order.getUserId());
    }

    @Override
    public Order getOrderById(Long orderId) {
        // 先查缓存
        String key = ORDER_CACHE_KEY + orderId;
        Order order = (Order) redisTemplate.opsForValue().get(key);
        
        if (order != null) {
            return order;
        }
        
        // 缓存未命中，查数据库
        order = orderMapper.selectById(orderId);
        if (order != null) {
            redisTemplate.opsForValue().set(key, order, CACHE_TTL, TimeUnit.MINUTES);
        }
        
        return order;
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        String key = USER_ORDERS_KEY + userId;
        
        // 查数据库
        List<Order> orders = orderMapper.selectByUserId(userId);
        
        return orders;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateOrderStatus(Long orderId, Integer status) {
        int updated = orderMapper.updateStatus(orderId, status);
        
        if (updated > 0) {
            // 更新缓存
            Order order = orderMapper.selectById(orderId);
            if (order != null) {
                String key = ORDER_CACHE_KEY + orderId;
                redisTemplate.opsForValue().set(key, order, CACHE_TTL, TimeUnit.MINUTES);
            }
            logger.info("订单状态更新：orderId={}, status={}", orderId, status);
            return true;
        }
        
        return false;
    }

    @Override
    public Order getOrderByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }
}
