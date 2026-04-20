# 服务注册发现与配置 - Nacos + Spring Cloud Gateway 实现

## 项目架构

本项目实现了基于 Nacos 的服务注册发现与配置管理，结合 Spring Cloud Gateway 实现服务网关和流量治理。

### 技术栈
- **Spring Boot 3.2.0**
- **Spring Cloud 2022.0.4**
- **Spring Cloud Alibaba 2022.0.0.0**
- **Nacos 2.2.3** (服务注册发现 + 配置中心)
- **Spring Cloud Gateway** (服务网关)
- **Resilience4j** (熔断、限流)
- **Sentinel** (流量控制)
- **Redis** (限流存储)

## 项目结构

```
Distributed-Software/
├── seckill-demo/
│   ├── gateway-service/         # 网关服务
│   │   ├── src/main/java/com/example/gateway/
│   │   │   ├── GatewayApplication.java        # 启动类
│   │   │   ├── config/                        # 配置类
│   │   │   │   ├── Resilience4jConfig.java    # 熔断、限流配置
│   │   │   │   ├── SentinelGatewayConfig.java # Sentinel 配置
│   │   │   │   └── NacosDynamicConfig.java    # Nacos 动态配置
│   │   │   └── controller/                    # 控制器
│   │   │       └── FallbackController.java    # 降级处理
│   │   ├── src/main/resources/
│   │   │   ├── application.yml               # 应用配置
│   │   │   ├── bootstrap.yml                 # 启动配置
│   │   │   ├── gateway-service.yaml          # Nacos 配置
│   │   │   └── gateway-dynamic-config.yaml   # 动态配置模板
│   │   └── pom.xml                           # 依赖配置
│   ├── nacos-docker/                         # Nacos Docker 配置
│   │   ├── docker-compose.yml               # Docker 启动配置
│   │   └── prometheus/
│   │       └── prometheus.yml               # 监控配置
│   ├── test-gateway.sh                      # 测试脚本
│   └── README.md                            # 项目文档
```

## 核心功能

### 1. 服务注册与发现
- 使用 Nacos 作为服务注册中心
- 服务自动注册到 Nacos
- 服务发现通过 Ribbon 负载均衡

### 2. 配置管理
- Nacos 作为配置中心
- 支持动态配置更新
- 配置分组管理 (SEKILL_GROUP)
- 多环境配置支持

### 3. 服务网关
- Spring Cloud Gateway 实现
- 动态路由配置
- 路径匹配与转发
- 跨域请求处理

### 4. 流量治理

#### 4.1 熔断机制 (Resilience4j)
- 基于失败率的熔断策略
- 滑动窗口统计
- 半开状态自动恢复
- 详细的事件监控

#### 4.2 限流机制
- 基于 Redis 的令牌桶算法
- 服务级限流配置
- 全局默认限流
- 突发流量处理

#### 4.3 降级处理
- 统一的降级接口
- 服务级别的降级策略
- 友好的降级响应

### 5. 动态配置能力
- Nacos 配置变更实时推送
- 运行时配置更新
- 配置版本管理
- 配置监控

## 配置说明

### Nacos 配置
- **服务名**: gateway-service
- **命名空间**: public
- **配置分组**: SEKILL_GROUP
- **配置文件**: gateway-service.yaml

### 动态配置 (gateway-dynamic-config)
```yaml
circuitBreaker:
  seckillCircuitBreaker:
    failureRateThreshold: 50
    waitDurationInOpenState: 10000
    slidingWindowSize: 10
rateLimiter:
  seckillRateLimiter:
    limitForPeriod: 100
    limitRefreshPeriod: 1000
```

## 服务端点

### 网关端点
- **健康检查**: `GET /actuator/health`
- **监控指标**: `GET /actuator/prometheus`
- **服务路由**: 
  - 秒杀服务: `POST /api/seckill`
  - 支付服务: `POST /api/payment`
  - 库存服务: `POST /api/stock`

### 降级端点
- **秒杀降级**: `GET /fallback/seckill`
- **支付降级**: `GET /fallback/payment`
- **库存降级**: `GET /fallback/stock`
- **全局降级**: `GET /fallback/global`

## 测试方法

### 1. 启动 Nacos
```bash
# 使用 Docker 启动
cd nacos-docker
docker-compose up -d

# 访问 Nacos 控制台
# http://localhost:8848/nacos
# 用户名: nacos, 密码: nacos
```

### 2. 启动网关服务
```bash
cd gateway-service
mvn spring-boot:run
```

### 3. 运行测试脚本
```bash
bash test-gateway.sh
```

### 4. JMeter 压力测试
1. **限流测试**: 100 并发用户，持续 30 秒
2. **熔断测试**: 200 并发用户，持续 60 秒
3. **降级测试**: 模拟服务故障场景

## 监控与告警

### Prometheus 监控
- **地址**: http://localhost:9090
- **指标**: 网关请求量、响应时间、错误率

### Grafana  dashboard
- **地址**: http://localhost:3000
- **账号**: admin, 密码: admin
- **面板**: 网关性能监控

## 流量治理效果

### 预期效果
1. **限流**: 超过阈值的请求被拒绝，返回 429 状态码
2. **熔断**: 服务故障时自动熔断，进入半开状态
3. **降级**: 服务不可用时返回降级响应
4. **动态配置**: 配置变更实时生效

### 测试场景
- **正常流量**: 所有请求正常处理
- **峰值流量**: 触发限流，部分请求被拒绝
- **服务故障**: 触发熔断，请求进入降级流程
- **配置变更**: 运行时调整限流阈值，立即生效

## 部署建议

### 生产环境
1. **Nacos 集群**: 至少 3 节点
2. **Gateway 集群**: 多实例部署
3. **Redis 集群**: 高可用配置
4. **监控告警**: 配置 Prometheus + Grafana 告警

### 开发环境
1. **单机 Nacos**: 快速启动
2. **本地 Redis**: 用于限流测试
3. **IDE 调试**: 支持热部署

## 总结

本项目完整实现了服务注册发现、配置管理、服务网关和流量治理的全流程。通过 Nacos 实现了服务的动态注册与配置管理，通过 Spring Cloud Gateway 实现了服务的统一入口和路由，通过 Resilience4j 和 Sentinel 实现了完善的流量治理能力。

该架构具有以下优势：
- **高可用性**: 服务注册发现确保服务的可靠发现
- **弹性伸缩**: 动态配置支持服务的弹性伸缩
- **流量保护**: 熔断、限流、降级三重保护
- **可观测性**: 完善的监控和告警机制
- **易于扩展**: 模块化设计，易于添加新服务

此架构适用于高并发、高可用的微服务系统，能够有效应对流量峰值和服务故障，保证系统的稳定运行。
