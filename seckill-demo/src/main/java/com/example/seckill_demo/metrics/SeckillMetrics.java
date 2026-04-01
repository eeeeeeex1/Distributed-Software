package com.example.seckill_demo.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 秒杀监控指标
 * 使用Micrometer + Prometheus进行监控
 */
@Component
public class SeckillMetrics {

    private final MeterRegistry meterRegistry;
    
    // 秒杀请求计数器
    private final Counter seckillRequestCounter;
    private final Counter seckillSuccessCounter;
    private final Counter seckillFailCounter;
    
    // 订单创建计数器
    private final Counter orderCreatedCounter;
    
    // 秒杀请求耗时
    private final Timer seckillTimer;
    
    // 库存指标
    private final AtomicLong currentStock = new AtomicLong(0);
    
    // Kafka消息堆积
    private final AtomicLong kafkaLag = new AtomicLong(0);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public SeckillMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 秒杀请求总数
        this.seckillRequestCounter = Counter.builder("seckill.request.total")
                .description("秒杀请求总数")
                .register(meterRegistry);
        
        // 秒杀成功数
        this.seckillSuccessCounter = Counter.builder("seckill.success.total")
                .description("秒杀成功数")
                .register(meterRegistry);
        
        // 秒杀失败数
        this.seckillFailCounter = Counter.builder("seckill.fail.total")
                .tag("reason", "unknown")
                .description("秒杀失败数")
                .register(meterRegistry);
        
        // 订单创建数
        this.orderCreatedCounter = Counter.builder("order.created.total")
                .description("订单创建总数")
                .register(meterRegistry);
        
        // 秒杀请求耗时
        this.seckillTimer = Timer.builder("seckill.request.duration")
                .description("秒杀请求耗时")
                .register(meterRegistry);
        
        // 库存Gauge
        Gauge.builder("seckill.stock.current", currentStock, AtomicLong::get)
                .description("当前库存")
                .register(meterRegistry);
        
        // Kafka消息堆积Gauge
        Gauge.builder("kafka.lag", kafkaLag, AtomicLong::get)
                .description("Kafka消息堆积")
                .register(meterRegistry);
    }
    
    /**
     * 记录秒杀请求
     */
    public void recordSeckillRequest() {
        seckillRequestCounter.increment();
    }
    
    /**
     * 记录秒杀成功
     */
    public void recordSeckillSuccess() {
        seckillSuccessCounter.increment();
    }
    
    /**
     * 记录秒杀失败
     */
    public void recordSeckillFail(String reason) {
        Counter.builder("seckill.fail.total")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * 记录订单创建
     */
    public void recordOrderCreated() {
        orderCreatedCounter.increment();
    }
    
    /**
     * 记录秒杀耗时
     */
    public void recordSeckillDuration(long durationMs) {
        seckillTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 更新库存指标
     */
    public void updateStockGauge(Long productId, long stock) {
        currentStock.set(stock);
    }
    
    /**
     * 更新Kafka堆积指标
     */
    public void updateKafkaLag(long lag) {
        kafkaLag.set(lag);
    }
    
    /**
     * 计时器
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void stopTimer(Timer.Sample sample) {
        sample.stop(seckillTimer);
    }
}
