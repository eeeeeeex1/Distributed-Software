package com.example.seckill_demo.service;

import java.util.Map;

/**
 * 对账服务接口
 */
public interface ReconciliationService {
    
    /**
     * 执行库存对账
     * @return 对账结果
     */
    Map<String, Object> reconcileStock();
    
    /**
     * 执行订单对账
     * @return 对账结果
     */
    Map<String, Object> reconcileOrders();
    
    /**
     * 修复库存不一致
     */
    void fixStockInconsistency(Long productId);
    
    /**
     * 定时对账任务
     */
    void scheduledReconciliation();
}
