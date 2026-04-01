package com.example.seckill_demo.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法(Snowflake)分布式唯一ID生成器
 * ID结构：0 - 41位时间戳 - 10位机器ID - 12位序列号
 * 总共64位，保证分布式环境下ID唯一性
 */
@Component
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1704067200000L; // 起始时间戳(2024-01-01 00:00:00)
    
    private static final long WORKER_ID_BITS = 5L;      // 机器ID位数
    private static final long DATACENTER_ID_BITS = 5L;  // 数据中心ID位数
    private static final long SEQUENCE_BITS = 12L;      // 序列号位数
    
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    
    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator() {
        this(1L, 1L);
    }
    
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID超出范围");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("Datacenter ID超出范围");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    
    /**
     * 生成唯一ID（线程安全）
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
    
    /**
     * 解析ID中的时间戳
     */
    public static long parseTimestamp(long id) {
        return (id >> 22) + EPOCH;
    }
    
    /**
     * 解析ID中的机器ID
     */
    public static long parseWorkerId(long id) {
        return (id >> 12) & 0x1F;
    }
    
    /**
     * 解析ID中的序列号
     */
    public static long parseSequence(long id) {
        return id & 0xFFF;
    }
    
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
