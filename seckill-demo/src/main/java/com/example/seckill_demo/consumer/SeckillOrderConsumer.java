package com.example.seckill_demo.consumer;

import com.example.seckill_demo.dto.SeckillMessage;
import com.example.seckill_demo.service.SeckillService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单Kafka消费者
 */
@Component
public class SeckillOrderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(SeckillOrderConsumer.class);
    
    @Autowired
    private SeckillService seckillService;

    /**
     * 消费秒杀订单消息
     */
    @KafkaListener(
            topics = "seckill-order",
            groupId = "seckill-order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSeckillOrder(ConsumerRecord<String, SeckillMessage> record, Acknowledgment ack) {
        SeckillMessage message = record.value();
        
        try {
            logger.info("收到秒杀订单消息：orderId={}, partition={}, offset={}", 
                    message.getOrderId(), record.partition(), record.offset());
            
            seckillService.processSeckillOrder(message);
            
            // 手动确认消息
            ack.acknowledge();
            
        } catch (Exception e) {
            logger.error("处理秒杀订单消息异常：orderId={}, error={}", 
                    message.getOrderId(), e.getMessage(), e);
            
            // 异常时也确认消息，避免重复消费，由重试机制处理
            ack.acknowledge();
        }
    }
    
    /**
     * 消费死信队列消息
     */
    @KafkaListener(
            topics = "seckill-order-dlt",
            groupId = "seckill-order-dlt-group"
    )
    public void consumeDeadLetterQueue(ConsumerRecord<String, SeckillMessage> record, Acknowledgment ack) {
        SeckillMessage message = record.value();
        
        logger.error("死信队列消息：orderId={}, userId={}, productId={}, retryCount={}", 
                message.getOrderId(), message.getUserId(), message.getProductId(), message.getRetryCount());
        
        // 记录到数据库或发送告警
        // TODO: 实现死信队列处理逻辑，如人工介入或定时重试
        
        ack.acknowledge();
    }
}
