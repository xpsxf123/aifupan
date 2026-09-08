package com.jiuyu.replay.common.cache;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 分布式锁降级服务，在Redis不可用时自动切换到本地JVM锁
 *
 * @author RayChou
 * @date 2025/7/2
 */
@Slf4j
@Component
public class HybridLockService {

    @Resource
    private RedissonClient redissonClient;

    // 本地锁映射
    private final ConcurrentHashMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    // 降级状态控制
    private final AtomicBoolean degraded = new AtomicBoolean(false);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);

    // 失败阈值，连续失败多少次后进入降级模式
    private static final int FAILURE_THRESHOLD = 3;
    // 重试间隔，多少毫秒后尝试恢复
    private static final long RETRY_INTERVAL_MS = 30000;

    /**
     * 尝试获取锁
     *
     * @param key            锁的键
     * @param timeoutSeconds 等待锁的超时时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String key, long timeoutSeconds) {
        // 如果未降级，尝试获取Redis分布式锁
        if (!isDegraded()) {
            try {
                RLock lock = redissonClient.getLock(key);
                boolean locked = lock.tryLock(timeoutSeconds, TimeUnit.SECONDS);
                resetFailures(); // 成功操作后重置失败计数
                return locked;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis锁操作异常，降级使用本地锁: {}", e.getMessage());
                recordFailure();
            }
        }

        // 降级模式：使用本地锁
        ReentrantLock localLock = localLocks.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            return localLock.tryLock(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Redis降级] 获取本地锁被中断: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param key 锁的键
     */
    public void unlock(String key) {
        // 如果未降级，尝试释放Redis锁
        if (!isDegraded()) {
            try {
                RLock lock = redissonClient.getLock(key);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    resetFailures();
                    return;
                }
            } catch (Exception e) {
                log.warn("[Redis降级] Redis解锁操作异常，尝试释放本地锁: {}", e.getMessage());
                recordFailure();
            }
        }

        // 尝试释放本地锁
        ReentrantLock localLock = localLocks.get(key);
        if (localLock != null && localLock.isHeldByCurrentThread()) {
            localLock.unlock();
        }
    }

    /**
     * 判断当前线程是否持有锁
     *
     * @param key 锁的键
     * @return 是否持有锁
     */
    public boolean isHeldByCurrentThread(String key) {
        // 如果未降级，检查Redis锁
        if (!isDegraded()) {
            try {
                RLock lock = redissonClient.getLock(key);
                boolean held = lock.isHeldByCurrentThread();
                resetFailures();
                return held;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis锁检查操作异常，检查本地锁: {}", e.getMessage());
                recordFailure();
            }
        }

        // 检查本地锁
        ReentrantLock localLock = localLocks.get(key);
        return localLock != null && localLock.isHeldByCurrentThread();
    }

    /**
     * 判断锁是否被任何线程持有
     *
     * @param key 锁的键
     * @return 是否被持有
     */
    public boolean isLocked(String key) {
        // 如果未降级，检查Redis锁
        if (!isDegraded()) {
            try {
                RLock lock = redissonClient.getLock(key);
                boolean locked = lock.isLocked();
                resetFailures();
                return locked;
            } catch (Exception e) {
                log.warn("[Redis降级] Redis锁检查操作异常，检查本地锁: {}", e.getMessage());
                recordFailure();
            }
        }

        // 检查本地锁
        ReentrantLock localLock = localLocks.get(key);
        return localLock != null && localLock.isLocked();
    }

    /**
     * 使用锁执行任务
     *
     * @param key            锁的键
     * @param timeoutSeconds 等待锁的超时时间（秒）
     * @param runnable       要执行的任务
     * @return 是否成功执行
     */
    public boolean executeWithLock(String key, long timeoutSeconds, Runnable runnable) {
        if (tryLock(key, timeoutSeconds)) {
            try {
                runnable.run();
                return true;
            } finally {
                unlock(key);
            }
        }
        return false;
    }

    /**
     * 判断是否处于降级状态
     *
     * @return 是否降级
     */
    public boolean isDegraded() {
        if (degraded.get()) {
            // 检查是否应该尝试恢复
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFailureTime.get() > RETRY_INTERVAL_MS) {
                // 超过重试间隔，但不在这里修改状态，而是让健康检查来处理
                // 这里只返回当前状态
                return true;
            }
            return true;
        }
        return false;
    }

    /**
     * 记录失败并可能触发降级
     */
    private void recordFailure() {
        lastFailureTime.set(System.currentTimeMillis());
        int currentFailures = consecutiveFailures.incrementAndGet();
        if (currentFailures >= FAILURE_THRESHOLD && !degraded.get()) {
            // 只有在之前不是降级状态时，才设置为降级并记录日志
            degraded.set(true);
            log.error("[Redis降级] Redis锁连续{}次操作失败，启用降级模式", currentFailures);
        }
    }

    /**
     * 重置失败计数
     */
    private void resetFailures() {
        // 只重置失败计数，不改变降级状态
        // 降级状态的改变由健康检查或手动设置负责
        consecutiveFailures.set(0);
    }

    /**
     * 手动设置降级状态
     *
     * @param degraded 是否降级
     */
    public void setDegraded(boolean degraded) {
        boolean oldState = this.degraded.get();
        this.degraded.set(degraded);

        if (degraded) {
            // 进入降级状态
            lastFailureTime.set(System.currentTimeMillis());
            if (!oldState) {
                log.info("[Redis降级] 手动启用锁服务降级模式");
            }
        } else {
            // 退出降级状态
            consecutiveFailures.set(0);
            if (oldState) {
                log.info("[Redis降级] 手动关闭锁服务降级模式");
            }
        }
    }

    /**
     * 检查是否应该退出降级状态
     * 此方法应由健康检查服务调用
     * 注意：此方法只负责状态检查和切换，清理工作统一由handleServiceRecovery处理
     *
     * @return 如果状态从降级变为非降级，返回true
     */
    public boolean checkAndExitDegradedMode() {
        if (degraded.get()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFailureTime.get() > RETRY_INTERVAL_MS) {
                // 超过重试间隔，尝试退出降级模式
                degraded.set(false);
                consecutiveFailures.set(0);
                log.info("[Redis降级] 超过重试间隔，退出锁服务降级模式");

                // 清理工作统一由handleServiceRecovery处理，这里不再执行
                return true;
            }
        }
        return false;
    }

    /**
     * 清理不再使用的本地锁
     */
    public void cleanupLocalLocks() {
        localLocks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
    }
} 