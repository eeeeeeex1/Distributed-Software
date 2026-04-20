package com.example.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/seckill")
    public Mono<ResponseEntity<Map<String, Object>>> seckillFallback() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("success", false);
        result.put("message", "秒杀服务暂时不可用，已进入熔断保护状态，请稍后重试");
        result.put("timestamp", System.currentTimeMillis());
        result.put("service", "seckill-service");
        result.put("fallback", true);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result));
    }

    @GetMapping("/payment")
    public Mono<ResponseEntity<Map<String, Object>>> paymentFallback() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("success", false);
        result.put("message", "支付服务暂时不可用，已进入熔断保护状态，请稍后重试");
        result.put("timestamp", System.currentTimeMillis());
        result.put("service", "payment-service");
        result.put("fallback", true);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result));
    }

    @GetMapping("/stock")
    public Mono<ResponseEntity<Map<String, Object>>> stockFallback() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("success", false);
        result.put("message", "库存服务暂时不可用，已进入熔断保护状态，请稍后重试");
        result.put("timestamp", System.currentTimeMillis());
        result.put("service", "stock-service");
        result.put("fallback", true);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result));
    }

    @GetMapping("/global")
    public Mono<ResponseEntity<Map<String, Object>>> globalFallback() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("success", false);
        result.put("message", "服务暂时不可用，请稍后重试");
        result.put("timestamp", System.currentTimeMillis());
        result.put("service", "gateway-service");
        result.put("fallback", true);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "gateway-service");
        result.put("timestamp", System.currentTimeMillis());

        return Mono.just(ResponseEntity.ok(result));
    }
}
