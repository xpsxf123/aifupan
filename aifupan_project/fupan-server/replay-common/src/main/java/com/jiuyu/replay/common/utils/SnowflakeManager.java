package com.jiuyu.replay.common.utils;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * 高并发优化的雪花ID生成器（支持静态批量获取） 参照源码Mybatis-plus 3.5.6实现
 *
 */
public class SnowflakeManager {
    // ============================== 基础参数 =================================
    /**
     * 起始时间戳 (2021-10-01 00:00:00 的毫秒数)
     */
    private static final long twepoch = 687888001020L;

    /**
     * 工作机器ID占用的位数
     */
    private static final long workerIdBits = 10L;
    /**
     * 序列号占用的位数
     */
    private static final long sequenceBits = 12L;

    /**
     * 支持的最大机器ID (0~1023)
     */
    private static final long maxWorkerId = ~(-1L << workerIdBits);
    /**
     * 序列号掩码 (防止溢出)
     */
    private static final long sequenceMask = ~(-1L << sequenceBits);

    /** 允许的时钟回拨最大值 (毫秒) */
    private static final long MAX_BACKWARD_MS = 10;

    // ============================== 移位参数 =================================
    /**
     * 时间戳向左移22位 (10+12)
     */
    private static final long timestampLeftShift = sequenceBits + workerIdBits;
    /**
     * 工作机器ID向左移12位
     */
    private static final long workerIdShift = sequenceBits;

    // ============================== 工作节点状态 ==============================
    /**
     * 工作机器ID (0~1023)
     */
    private final long workerId;

    /**
     * 时间戳与序列号状态
     */
    private final AtomicLong state = new AtomicLong(0);

    // ============================== 单例模式 =================================
    private static class Holder {
        private static final SnowflakeManager INSTANCE = new SnowflakeManager();
    }

    public static SnowflakeManager getInstance() {
        return Holder.INSTANCE;
    }

    private SnowflakeManager() {
        this.workerId = generateWorkerId();
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException(
                    String.format("Worker ID must be between 0 and %d, but got %d", maxWorkerId, workerId)
            );
        }
        System.out.println("Snowflake initialized. WorkerId: " + workerId);
    }

    /**
     * 自动生成工作节点ID
     */
    private long generateWorkerId() {
        try {
            // 组合机器信息生成哈希值
            String machineId = getMachineIdentifier() + getJvmPid();
            int hash = machineId.hashCode();
            long workerId = Math.abs(hash) % (maxWorkerId + 1);
            return workerId;
        } catch (Exception e) {
            return fallbackWorkerId();
        }
    }

    /**
     * 获取机器唯一标识
     */
    private String getMachineIdentifier() throws Exception {
        StringBuilder sb = new StringBuilder();

        // 获取主机名
        String hostName = InetAddress.getLocalHost().getHostName();
        sb.append(hostName);

        // 获取MAC地址
        NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        if (ni != null) {
            byte[] mac = ni.getHardwareAddress();
            if (mac != null) {
                for (byte b : mac) {
                    sb.append(String.format("%02X", b));
                }
            }
        }
        return sb.toString();
    }

    /**
     * 获取JVM进程ID
     */
    private String getJvmPid() {
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            return jvmName.split("@")[0];
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * 回退方案生成workerId
     */
    private long fallbackWorkerId() {
        String fallbackId = System.getProperty("user.name")
                + System.getProperty("os.name")
                + System.getProperty("os.arch");
        int hash = fallbackId.hashCode();
        return Math.abs(hash) % (maxWorkerId + 1);
    }

    /**
     * 高并发优化的ID生成（单次获取）
     */
    public long nextId() {
        while (true) {
            long currentState = state.get();
            // 解析状态：高52位是时间戳，低12位是序列号
            long timestampPart = currentState >>> sequenceBits;
            long sequence = currentState & sequenceMask;

            long currentTime = timeGen();
            long timestamp = timestampPart + twepoch;

            // 处理时钟回拨
            if (currentTime < timestamp) {
                long offset = timestamp - currentTime;
                if (offset <= MAX_BACKWARD_MS) {
                    // 在允许范围内等待
                    LockSupport.parkNanos(offset * 2 * 1_000_000);
                    continue;
                } else {
                    throw new IllegalStateException(
                            String.format("Clock moved backwards. Refusing for %d ms", offset)
                    );
                }
            }

            // 检查是否需要重置序列号
            if (currentTime > timestamp) {
                // 新时间周期，重置序列号
                sequence = 0;
                timestamp = currentTime;
            } else if (sequence == sequenceMask) {
                // 当前毫秒序列号已用完，等待下一毫秒
                currentTime = tilNextMillis(timestamp);
                sequence = 0;
                timestamp = currentTime;
            }

            // 尝试更新状态
            long newState = ((timestamp - twepoch) << sequenceBits) | (sequence + 1);
            if (state.compareAndSet(currentState, newState)) {
                return ((timestamp - twepoch) << timestampLeftShift)
                        | (workerId << workerIdShift)
                        | sequence;
            }
            // CAS失败，重试
        }
    }

    /**
     * 批量获取ID（高性能）
     *
     * @param count 要获取的ID数量（1-4096）
     * @return 批量ID列表
     */
    public List<Long> nextBatchId(int count) {
        // 参数校验
        if (count <= 0 || count > 4096) {
            throw new IllegalArgumentException("Count must be between 1 and 4096");
        }

        List<Long> ids = new ArrayList<>(count);
        int generated = 0;

        while (generated < count) {
            long currentState = state.get();
            // 解析状态：高52位是时间戳，低12位是序列号
            long timestampPart = currentState >>> sequenceBits;
            long sequence = currentState & sequenceMask;

            long currentTime = timeGen();
            long timestamp = timestampPart + twepoch;

            // 处理时钟回拨
            if (currentTime < timestamp) {
                long offset = timestamp - currentTime;
                if (offset <= MAX_BACKWARD_MS) {
                    LockSupport.parkNanos(offset * 2 * 1_000_000);
                    continue;
                } else {
                    throw new IllegalStateException(
                            String.format("Clock moved backwards. Refusing for %d ms", offset)
                    );
                }
            }

            // 计算当前毫秒剩余可用序列号
            long remainingInMs = sequenceMask - sequence;
            if (remainingInMs <= 0) {
                // 当前毫秒已用完，等待下一毫秒
                currentTime = tilNextMillis(timestamp);
                sequence = 0;
                timestamp = currentTime;
                remainingInMs = sequenceMask + 1; // 4096
            }

            // 本次可生成的ID数量
            int batchSize = (int) Math.min(count - generated, remainingInMs);

            // 生成ID范围
            long baseId = ((timestamp - twepoch) << timestampLeftShift)
                    | (workerId << workerIdShift)
                    | sequence;

            // 添加批量ID
            for (int i = 0; i < batchSize; i++) {
                ids.add(baseId + i);
            }

            // 尝试更新状态
            long newSequence = sequence + batchSize;
            long newState = ((timestamp - twepoch) << sequenceBits) | newSequence;

            if (state.compareAndSet(currentState, newState)) {
                generated += batchSize;
            }
            // CAS失败会重试
        }

        return ids;
    }

    /**
     * 阻塞到下一毫秒
     * @param lastTimestamp 上次生成ID的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            // 使用更高效的等待方式
            long delta = lastTimestamp - timestamp + 1;
            LockSupport.parkNanos(delta * 1_000_000);
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前毫秒数
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    // ============================== 对外静态方法 ==============================

    /**
     * 静态方法：获取单个ID
     */
    public static long nextValue() {
        return getInstance().nextId();
    }

    /**
     * 静态方法：批量获取ID
     * @param count 要生成的ID数量（1-4096）
     * @return 批量ID列表
     */
    public static List<Long> nextBatch(int count) {
        return getInstance().nextBatchId(count);
    }
}