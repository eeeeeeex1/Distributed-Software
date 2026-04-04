package com.example.seckill_demo.service;

import com.example.seckill_demo.dto.*;

/**
 * 事务协调服务接口
 * 用于管理分布式事务的一致性
 */
public interface TransactionCoordinatorService {
    
    /**
     * 开始秒杀事务
     */
    TransactionResult beginSeckillTransaction(SeckillMessage message);
    
    /**
     * 提交秒杀事务
     */
    boolean commitSeckillTransaction(String transactionId);
    
    /**
     * 回滚秒杀事务
     */
    boolean rollbackSeckillTransaction(String transactionId);
    
    /**
     * 开始支付事务
     */
    TransactionResult beginPaymentTransaction(PaymentMessage message);
    
    /**
     * 提交支付事务
     */
    boolean commitPaymentTransaction(String transactionId);
    
    /**
     * 回滚支付事务
     */
    boolean rollbackPaymentTransaction(String transactionId);
    
    /**
     * 处理超时事务
     */
    void handleTimeoutTransactions();
    
    /**
     * 检查事务状态
     */
    TransactionStatus getTransactionStatus(String transactionId);
}
