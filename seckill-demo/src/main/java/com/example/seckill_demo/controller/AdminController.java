package com.example.seckill_demo.controller;

import com.example.seckill_demo.security.SecurityService;
import com.example.seckill_demo.service.ReconciliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 管理控制器
 * 提供系统管理、监控、安全配置等接口
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private SecurityService securityService;
    
    @Autowired
    private ReconciliationService reconciliationService;

    /**
     * 添加IP到黑名单
     */
    @PostMapping("/security/blacklist/{ip}")
    public Map<String, Object> addToBlacklist(@PathVariable String ip) {
        Map<String, Object> result = new HashMap<>();
        securityService.addToBlacklist(ip);
        result.put("code", 200);
        result.put("message", "IP已加入黑名单");
        return result;
    }
    
    /**
     * 从黑名单移除IP
     */
    @DeleteMapping("/security/blacklist/{ip}")
    public Map<String, Object> removeFromBlacklist(@PathVariable String ip) {
        Map<String, Object> result = new HashMap<>();
        securityService.removeFromBlacklist(ip);
        result.put("code", 200);
        result.put("message", "IP已从黑名单移除");
        return result;
    }
    
    /**
     * 获取黑名单列表
     */
    @GetMapping("/security/blacklist")
    public Map<String, Object> getBlacklist() {
        Map<String, Object> result = new HashMap<>();
        Set<Object> blacklist = securityService.getBlacklist();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", blacklist);
        return result;
    }
    
    /**
     * 执行库存对账
     */
    @GetMapping("/reconciliation/stock")
    public Map<String, Object> reconcileStock() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> reconcileResult = reconciliationService.reconcileStock();
        result.put("code", 200);
        result.put("message", "对账完成");
        result.put("data", reconcileResult);
        return result;
    }
    
    /**
     * 修复库存不一致
     */
    @PostMapping("/reconciliation/stock/fix/{productId}")
    public Map<String, Object> fixStock(@PathVariable Long productId) {
        Map<String, Object> result = new HashMap<>();
        reconciliationService.fixStockInconsistency(productId);
        result.put("code", 200);
        result.put("message", "库存已修复");
        return result;
    }
    
    /**
     * 生成验证码
     */
    @GetMapping("/captcha/{userId}")
    public Map<String, Object> generateCaptcha(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        String captcha = securityService.generateCaptcha(userId);
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", Map.of("captcha", captcha));
        return result;
    }
}
