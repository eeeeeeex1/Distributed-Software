#!/bin/bash

# 测试脚本 - 网关服务功能测试

# 测试URLs
GATEWAY_URL="http://localhost:8080"
SECKILL_ENDPOINT="${GATEWAY_URL}/api/seckill"
PAYMENT_ENDPOINT="${GATEWAY_URL}/api/payment"
STOCK_ENDPOINT="${GATEWAY_URL}/api/stock"
FALLBACK_ENDPOINT="${GATEWAY_URL}/fallback"
HEALTH_ENDPOINT="${GATEWAY_URL}/actuator/health"

# 测试结果
TEST_RESULTS=""

# 测试函数
run_test() {
    local name="$1"
    local url="$2"
    local method="${3:-GET}"
    local body="${4:-}"
    
    echo "\n=== 测试: $name ==="
    echo "URL: $url"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "%{http_code}" -o /dev/null "$url")
    else
        response=$(curl -s -w "%{http_code}" -o /dev/null -X "$method" -H "Content-Type: application/json" -d "$body" "$url")
    fi
    
    echo "响应码: $response"
    
    if [ "$response" -ge 200 ] && [ "$response" -lt 400 ]; then
        echo "✓ 测试通过"
        TEST_RESULTS="$TEST_RESULTS\n✓ $name"
    else
        echo "✗ 测试失败"
        TEST_RESULTS="$TEST_RESULTS\n✗ $name"
    fi
}

# 运行所有测试
echo "开始测试网关服务..."
echo "=================================="

# 1. 健康检查
run_test "健康检查" "$HEALTH_ENDPOINT"

# 2. 服务路由测试
run_test "秒杀服务路由" "$SECKILL_ENDPOINT"
run_test "支付服务路由" "$PAYMENT_ENDPOINT"
run_test "库存服务路由" "$STOCK_ENDPOINT"

# 3. 降级功能测试
run_test "秒杀服务降级" "$FALLBACK_ENDPOINT/seckill"
run_test "支付服务降级" "$FALLBACK_ENDPOINT/payment"
run_test "库存服务降级" "$FALLBACK_ENDPOINT/stock"

# 4. 全局降级测试
run_test "全局降级" "$FALLBACK_ENDPOINT/global"

# 5. 压力测试准备
echo "\n=== 压力测试准备 ==="
echo "建议使用JMeter进行以下测试："
echo "1. 并发100用户，持续30秒，测试限流效果"
echo "2. 并发200用户，持续60秒，测试熔断效果"
echo "3. 模拟服务故障，测试降级效果"

# 显示测试结果
echo "\n=================================="
echo "测试结果汇总:"
echo "=================================="
echo "$TEST_RESULTS"
echo "=================================="
echo "测试完成！"
