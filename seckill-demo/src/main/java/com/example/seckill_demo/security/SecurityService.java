package com.example.seckill_demo.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 安全防护组件
 * 实现IP黑名单、限流、防刷机制
 */
@Component
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    
    // Redis Key前缀
    private static final String IP_BLACKLIST_KEY = "security:ip:blacklist";
    private static final String IP_REQUEST_COUNT_KEY = "security:ip:request:";
    private static final String USER_REQUEST_COUNT_KEY = "security:user:request:";
    private static final String CAPTCHA_KEY = "security:captcha:";
    
    // 限流阈值
    private static final int IP_REQUEST_LIMIT = 100;       // IP每秒最大请求数
    private static final int USER_REQUEST_LIMIT = 10;      // 用户每秒最大请求数
    private static final int IP_BLACKLIST_THRESHOLD = 1000; // 加入黑名单的请求阈值
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 检查IP是否在黑名单中
     */
    public boolean isIpBlacklisted(String ip) {
        return redisTemplate.opsForSet().isMember(IP_BLACKLIST_KEY, ip);
    }
    
    /**
     * 添加IP到黑名单
     */
    public void addToBlacklist(String ip) {
        redisTemplate.opsForSet().add(IP_BLACKLIST_KEY, ip);
        logger.warn("IP已加入黑名单: {}", ip);
    }
    
    /**
     * 从黑名单移除IP
     */
    public void removeFromBlacklist(String ip) {
        redisTemplate.opsForSet().remove(IP_BLACKLIST_KEY, ip);
        logger.info("IP已从黑名单移除: {}", ip);
    }
    
    /**
     * 获取所有黑名单IP
     */
    public Set<Object> getBlacklist() {
        return redisTemplate.opsForSet().members(IP_BLACKLIST_KEY);
    }
    
    /**
     * IP限流检查
     * @return true-允许访问，false-被限流
     */
    public boolean checkIpRateLimit(String ip) {
        String key = IP_REQUEST_COUNT_KEY + ip;
        
        // 检查黑名单
        if (isIpBlacklisted(ip)) {
            logger.warn("IP在黑名单中，拒绝访问: {}", ip);
            return false;
        }
        
        // 获取当前请求计数
        Object countObj = redisTemplate.opsForValue().get(key);
        int count = countObj == null ? 0 : Integer.parseInt(countObj.toString());
        
        if (count >= IP_REQUEST_LIMIT) {
            logger.warn("IP请求过于频繁，限流: ip={}, count={}", ip, count);
            
            // 超过阈值加入黑名单
            if (count >= IP_BLACKLIST_THRESHOLD) {
                addToBlacklist(ip);
            }
            
            return false;
        }
        
        // 增加请求计数
        if (count == 0) {
            redisTemplate.opsForValue().set(key, 1, 1, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(key);
        }
        
        return true;
    }
    
    /**
     * 用户限流检查
     * @return true-允许访问，false-被限流
     */
    public boolean checkUserRateLimit(Long userId) {
        String key = USER_REQUEST_COUNT_KEY + userId;
        
        Object countObj = redisTemplate.opsForValue().get(key);
        int count = countObj == null ? 0 : Integer.parseInt(countObj.toString());
        
        if (count >= USER_REQUEST_LIMIT) {
            logger.warn("用户请求过于频繁，限流: userId={}, count={}", userId, count);
            return false;
        }
        
        if (count == 0) {
            redisTemplate.opsForValue().set(key, 1, 1, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().increment(key);
        }
        
        return true;
    }
    
    /**
     * 生成验证码
     */
    public String generateCaptcha(Long userId) {
        String captcha = String.valueOf((int) ((Math.random() * 9000) + 1000));
        String key = CAPTCHA_KEY + userId;
        redisTemplate.opsForValue().set(key, captcha, 5, TimeUnit.MINUTES);
        logger.info("生成验证码: userId={}, captcha={}", userId, captcha);
        return captcha;
    }
    
    /**
     * 验证验证码
     */
    public boolean verifyCaptcha(Long userId, String captcha) {
        String key = CAPTCHA_KEY + userId;
        Object storedCaptcha = redisTemplate.opsForValue().get(key);
        
        if (storedCaptcha != null && storedCaptcha.toString().equals(captcha)) {
            redisTemplate.delete(key);
            return true;
        }
        
        return false;
    }
    
    /**
     * 综合安全检查
     */
    public SecurityCheckResult checkSecurity(String ip, Long userId) {
        // IP黑名单检查
        if (isIpBlacklisted(ip)) {
            return new SecurityCheckResult(false, "IP已被封禁");
        }
        
        // IP限流检查
        if (!checkIpRateLimit(ip)) {
            return new SecurityCheckResult(false, "IP请求过于频繁");
        }
        
        // 用户限流检查
        if (userId != null && !checkUserRateLimit(userId)) {
            return new SecurityCheckResult(false, "用户请求过于频繁");
        }
        
        return new SecurityCheckResult(true, "安全检查通过");
    }
    
    /**
     * 安全检查结果
     */
    public static class SecurityCheckResult {
        private final boolean allowed;
        private final String message;
        
        public SecurityCheckResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
