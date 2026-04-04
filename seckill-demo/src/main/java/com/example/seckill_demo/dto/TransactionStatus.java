package com.example.seckill_demo.dto;

/**
 * 事务状态枚举
 */
public enum TransactionStatus {
    
    INITIATED("初始化"),      // 事务已初始化
    PENDING("待处理"),         // 事务待处理
    PROCESSING("处理中"),      // 事务处理中
    SUCCESS("成功"),          // 事务成功
    FAILED("失败"),           // 事务失败
    TIMEOUT("超时"),          // 事务超时
    ROLLBACKED("已回滚");     // 事务已回滚
    
    private final String description;
    
    TransactionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
