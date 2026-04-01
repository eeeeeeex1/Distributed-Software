package com.example.seckill_demo.service;

import com.example.seckill_demo.dto.SeckillMessage;
import com.example.seckill_demo.dto.SeckillResult;

/**
 * 秒杀服务接口
 */
public interface SeckillService {
    
    /**
     * 执行秒杀（入口方法）
     * 1. 幂等性校验
     * 2. 库存预扣减
     * 3. 发送Kafka消息
     */
    SeckillResult doSeckill(Long userId, Long productId, Integer quantity);
    
    /**
     * 异步处理秒杀订单（Kafka消费者调用）
     */
    void processSeckillOrder(SeckillMessage message);
    
    /**
     * 检查用户秒杀资格
     */
    boolean checkSeckillQualification(Long userId, Long productId);
    
    /**
     * 标记用户已秒杀
     */
    void markUserSeckilled(Long userId, Long productId);
    
    /**
     * 库存预热（活动开始前调用）
     */
    void warmUpAllStock();
}
