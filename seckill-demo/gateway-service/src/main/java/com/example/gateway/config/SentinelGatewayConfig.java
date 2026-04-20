package com.example.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;       
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;    
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager; 
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;       
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class SentinelGatewayConfig {

    @Bean
    public SentinelGatewayFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    @PostConstruct
    public void init() {
        initGatewayRules();
        initCustomizedApis();
        initBlockHandlers();
    }

    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        rules.add(new GatewayFlowRule("seckill-api")
                .setCount(100)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(0)
                )
        );

        rules.add(new GatewayFlowRule("payment-api")
                .setCount(50)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(0)
                )
        );

        rules.add(new GatewayFlowRule("stock-api")
                .setCount(200)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(0)
                )
        );

        GatewayRuleManager.loadRules(rules);
    }

    private void initCustomizedApis() {
        Set<ApiDefinition> definitions = new HashSet<>();

        ApiDefinition seckillApi = new ApiDefinition("seckill-api")
                .setPredicateItems(new HashSet<>(Arrays.asList(
                        new ApiPathPredicateItem().setPattern("/seckill/**")    
                )));

        ApiDefinition paymentApi = new ApiDefinition("payment-api")
                .setPredicateItems(new HashSet<>(Arrays.asList(
                        new ApiPathPredicateItem().setPattern("/payment/**")    
                )));

        ApiDefinition stockApi = new ApiDefinition("stock-api")
                .setPredicateItems(new HashSet<>(Arrays.asList(
                        new ApiPathPredicateItem().setPattern("/stock/**")      
                )));

        definitions.add(seckillApi);
        definitions.add(paymentApi);
        definitions.add(stockApi);

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    private void initBlockHandlers() {
        BlockRequestHandler blockHandler = (serverWebExchange, throwable) -> {  
            Map<String, Object> result = new HashMap<>();
            result.put("code", 429);
            result.put("success", false);
            result.put("message", "服务已被限流或熔断，请稍后重试"); 
            result.put("timestamp", System.currentTimeMillis());
            result.put("error", throwable.getClass().getSimpleName());

            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(result);
        };

        GatewayCallbackManager.setBlockHandler(blockHandler);
    }
}