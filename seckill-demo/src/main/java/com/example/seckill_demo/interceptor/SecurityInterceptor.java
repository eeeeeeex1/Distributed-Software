package com.example.seckill_demo.interceptor;

import com.example.seckill_demo.security.SecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * 安全拦截器
 * 实现IP黑名单、限流检查
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private SecurityService securityService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        
        // 只对秒杀接口进行安全检查
        if (uri.startsWith("/api/seckill/")) {
            // 从请求中获取用户ID（简化处理，实际应从Token中解析）
            Long userId = extractUserId(request);
            
            SecurityService.SecurityCheckResult checkResult = securityService.checkSecurity(ip, userId);
            
            if (!checkResult.isAllowed()) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(429);
                
                Map<String, Object> result = new HashMap<>();
                result.put("code", 429);
                result.put("message", checkResult.getMessage());
                
                response.getWriter().write(objectMapper.writeValueAsString(result));
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 从请求中提取用户ID
     */
    private Long extractUserId(HttpServletRequest request) {
        // 简化处理：从请求头获取
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr != null) {
            try {
                return Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
