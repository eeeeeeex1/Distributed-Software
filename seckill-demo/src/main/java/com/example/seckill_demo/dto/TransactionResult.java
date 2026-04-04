package com.example.seckill_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 事务结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String transactionId;      // 事务ID
    private boolean success;          // 是否成功
    private String message;           // 消息
    private TransactionStatus status;  // 事务状态
    
    public static TransactionResult success(String transactionId, TransactionStatus status) {
        return new TransactionResult(transactionId, true, "事务开始成功", status);
    }
    
    public static TransactionResult failure(String message) {
        return new TransactionResult(null, false, message, TransactionStatus.FAILED);
    }
}
