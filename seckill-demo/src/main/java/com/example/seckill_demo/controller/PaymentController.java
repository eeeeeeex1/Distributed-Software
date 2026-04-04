package com.example.seckill_demo.controller;

import com.example.seckill_demo.dto.TransactionResult;
import com.example.seckill_demo.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付控制器
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentService paymentService;

    /**
     * 发起支付
     */
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiatePayment(
            @RequestParam Long orderId,
            @RequestParam Long userId,
            @RequestParam String paymentMethod) {
        
        try {
            logger.info("发起支付请求：orderId={}, userId={}, paymentMethod={}", 
                    orderId, userId, paymentMethod);
            
            TransactionResult result = paymentService.initiatePayment(orderId, userId, paymentMethod);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            
            if (result.isSuccess()) {
                response.put("transactionId", result.getTransactionId());
                response.put("status", result.getStatus().name());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("发起支付异常：orderId={}, userId={}, error={}", 
                    orderId, userId, e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "系统错误，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 处理支付回调
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleCallback(
            @RequestParam String transactionId,
            @RequestParam String paymentStatus,
            @RequestParam String paymentNo) {
        
        try {
            logger.info("收到支付回调：transactionId={}, status={}, paymentNo={}", 
                    transactionId, paymentStatus, paymentNo);
            
            boolean success = paymentService.handlePaymentCallback(transactionId, paymentStatus, paymentNo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "回调处理成功" : "回调处理失败");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("处理支付回调异常：transactionId={}, error={}", 
                    transactionId, e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "系统错误，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 检查支付状态
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(
            @PathVariable String transactionId) {
        
        try {
            logger.info("检查支付状态：transactionId={}", transactionId);
            
            String status = paymentService.checkPaymentStatus(transactionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactionId", transactionId);
            response.put("status", status);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查支付状态异常：transactionId={}, error={}", 
                    transactionId, e.getMessage(), e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "系统错误，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
