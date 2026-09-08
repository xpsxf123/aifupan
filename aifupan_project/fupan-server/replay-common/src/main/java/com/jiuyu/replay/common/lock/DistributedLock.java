package com.jiuyu.replay.common.lock;

import com.jiuyu.replay.common.utils.RRException;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁简单调用工具类
 *
 * @author RayChou
 * @date 2025/6/20 11:00
 */
@Slf4j
@Component
public class DistributedLock {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 尝试获取锁并执行业务逻辑
     *
     * @param lockKey   锁的key
     * @param supplier  业务逻辑
     * @param waitTime  等待锁的最大时间（秒）
     * @param leaseTime 锁的自动释放时间（秒）
     * @param <T>       返回值类型
     * @return 业务逻辑的返回值
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier, int waitTime, int leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁
            boolean isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("executeWithLock-supplier获取分布式锁失败，Key: {}", lockKey);
                throw new BusinessException(StatusCode.REDIS_LOCK_ERROR);
            }
            // 执行业务逻辑
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，Key: {}", lockKey, e);
            throw new BusinessException(StatusCode.REDIS_LOCK_INTERRUPT);
        } catch (BusinessException | RRException e) {
            // 直接抛出锁相关的异常
            throw e;
        } catch (Exception e) {
            log.error("执行业务逻辑异常，Key: {}", lockKey, e);
            throw e;
        } finally {
            // 释放锁（确保当前线程持有锁）
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 尝试获取锁并执行业务逻辑（无返回值）
     *
     * @param lockKey   锁的key
     * @param runnable  业务逻辑
     * @param waitTime  等待锁的最大时间（秒）
     * @param leaseTime 锁的自动释放时间（秒）
     */
    public void executeWithLock(String lockKey, Runnable runnable, int waitTime, int leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁
            boolean isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("executeWithLock-runnable获取分布式锁失败，Key: {}", lockKey);
                throw new BusinessException(StatusCode.REDIS_LOCK_ERROR);
            }
            // 执行业务逻辑
            runnable.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断，Key: {}", lockKey, e);
            throw new BusinessException(StatusCode.REDIS_LOCK_INTERRUPT);
        } catch (BusinessException e) {
            // 直接抛出锁相关的异常
            throw e;
        } catch (Exception e) {
            log.error("执行业务逻辑异常，Key: {}", lockKey, e);
            throw e;
        } finally {
            // 释放锁（确保当前线程持有锁）
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 使用默认超时时间尝试获取锁并执行业务逻辑
     *
     * @param lockKey  锁的key
     * @param supplier 业务逻辑
     * @param <T>      返回值类型
     * @return 业务逻辑的返回值
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, supplier, 10, 30);
    }

    /**
     * 使用默认超时时间尝试获取锁并执行业务逻辑（无返回值）
     *
     * @param lockKey  锁的key
     * @param runnable 业务逻辑
     */
    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, runnable, 10, 30);
    }
} 