package com.example.seckill_demo.service;

/**
 * 库存服务接口
 */
public interface StockService {
    
    /**
     * 库存预热：将商品库存加载到Redis
     */
    void warmUpStock(Long productId, Integer stock);
    
    /**
     * 预扣减库存（原子操作）
     * @return 扣减后的库存，-1表示库存不足
     */
    Long deductStock(Long productId, Integer quantity);
    
    /**
     * 回滚库存（订单失败时调用）
     */
    void rollbackStock(Long productId, Integer quantity);
    
    /**
     * 获取当前库存
     */
    Integer getCurrentStock(Long productId);
    
    /**
     * 同步数据库库存到Redis
     */
    void syncStockFromDB(Long productId);
    
    /**
     * 库存对账：检查Redis和数据库库存一致性
     */
    boolean checkStockConsistency(Long productId);
}
