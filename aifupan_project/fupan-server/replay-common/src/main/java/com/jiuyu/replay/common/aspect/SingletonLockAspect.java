package com.jiuyu.replay.common.aspect;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.annotation.SingletonLock;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/14 下午2:43
 */
@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class SingletonLockAspect {
    private final RedissonClient redissonClient;

    @Around("@annotation(singletonLock)")
    public Object around(ProceedingJoinPoint joinPoint, SingletonLock singletonLock) throws Throwable {
        long expireTime = singletonLock.expireTime();
        long waitTime = singletonLock.waitTime();
        String key = singletonLock.prefixKey();
        String keyValue = singletonLock.key();

        if (ObjectUtil.isNotEmpty(keyValue)) {
            if (keyValue.startsWith("#")) {
                // 解析SpEL
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Object[] args = joinPoint.getArgs();
                String[] parameterNames = signature.getParameterNames();

                if (parameterNames != null && parameterNames.length > 0){
                    // 使用SpEL解析表达式
                    SpelExpressionParser parser = new SpelExpressionParser();
                    Expression expression = parser.parseExpression(keyValue);
                    StandardEvaluationContext context = new StandardEvaluationContext();
                    for (int i = 0; i < parameterNames.length; i++) {
                        String fieldName = parameterNames[i];
                        context.setVariable(fieldName, args[i]);
                    }
                    String value = "";
                    try{
                        value = expression.getValue(context, String.class);
                    }catch (Exception e){
                        log.info("表达式解析失败：" + e.getMessage());
                    }

                    key = key + "_" + value;
                }
            } else {
                if (ObjectUtil.isNotEmpty(key)){
                    key = key + "_" + keyValue;
                }
            }
        }

        RLock lock = redissonClient.getLock(key);

        try {
            // 尝试获取锁，等待指定的时间，超过时间自动返回失败
            boolean locked = lock.tryLock(waitTime, expireTime, TimeUnit.SECONDS);

            if (!locked) {
                log.info("无法获取分布式锁，已超时");
                throw new RuntimeException("无法获取分布式锁，已超时");
            }

            // 执行目标方法
            return joinPoint.proceed();
        } finally {
            // 方法执行完毕后释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
