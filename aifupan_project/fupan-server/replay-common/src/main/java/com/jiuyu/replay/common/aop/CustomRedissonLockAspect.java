package com.jiuyu.replay.common.aop;

import com.jiuyu.replay.common.annotation.CustomRedissonLock;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面（支持事务感知）
 * 在事务中执行时，锁会在事务提交后释放，保证数据一致性
 *
 * @author RayChou
 * @date 2025-11-24
 */
@Slf4j
@Aspect
@Component
public class CustomRedissonLockAspect {

    @Resource
    private RedissonClient redissonClient;

    @Around("@annotation(customRedissonLock)")
    public Object around(ProceedingJoinPoint joinPoint, CustomRedissonLock customRedissonLock) throws Throwable {
        String lockKey = resolveLockKey(joinPoint, customRedissonLock.key());
        RLock lock = redissonClient.getLock(lockKey);
        boolean isInTransaction = false;
        try {
            boolean isLocked = lock.tryLock(customRedissonLock.waitTime(), customRedissonLock.leaseTime(), TimeUnit.SECONDS);
            if (!isLocked) {
                log.error("获取分布式锁失败，Key: {}", lockKey);
                throw new BusinessException("获取分布式锁失败，Key: " + lockKey);
            }
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                isInTransaction = true;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                });
            }
            return joinPoint.proceed();
        } finally {
            if (!isInTransaction && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 解析锁的Key（支持Spring EL表达式）
     * 例如：key = "lock:#{T(java.util.UUID).randomUUID()}"
     */
    private String resolveLockKey(ProceedingJoinPoint joinPoint, String key) {
        if (key.isEmpty()) {
            // 默认使用类名+方法名+参数的MD5（保证唯一性）
            return String.format(
                    "%s#%s_%s",
                    joinPoint.getTarget().getClass().getName(),
                    joinPoint.getSignature().getName(),
                    joinPoint.getArgs().hashCode()
            );
        } else {
            // 使用SpEL解析表达式
            SpelExpressionParser parser = new SpelExpressionParser();
            // 表达式内容不包含 #{} 边界符
            Expression expression = parser.parseExpression(key);
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("args", joinPoint.getArgs());
            return expression.getValue(context, String.class);
        }
    }
}
