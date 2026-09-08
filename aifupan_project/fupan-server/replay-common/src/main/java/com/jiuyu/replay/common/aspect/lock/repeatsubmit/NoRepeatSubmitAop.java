package com.jiuyu.replay.common.aspect.lock.repeatsubmit;

import com.jiuyu.replay.common.utils.RRException;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 防止重复提交AOP实现
 *
 * @author RayChou
 * @date 2025/6/6 18:27
 */
@Aspect
@Component
public class NoRepeatSubmitAop {

    private static final Logger log = LoggerFactory.getLogger(NoRepeatSubmitAop.class);

    @Resource
    private RedissonClient redissonClient;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(noRepeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, NoRepeatSubmit noRepeatSubmit) throws Throwable {
        String lockKey;

        try {
            // 尝试解析SpEL表达式
            String spElKey = noRepeatSubmit.key();
            if (!StringUtils.isEmpty(spElKey)) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                Object[] args = joinPoint.getArgs();
                String[] parameterNames = signature.getParameterNames();

                EvaluationContext context = new StandardEvaluationContext();
                if (parameterNames != null) {
                    for (int i = 0; i < parameterNames.length; i++) {
                        context.setVariable(parameterNames[i], args[i]);
                    }
                }

                lockKey = parser.parseExpression(spElKey).getValue(context, String.class);
                // 防止解析结果为null
                if (lockKey == null) {
                    lockKey = noRepeatSubmit.prefix() + joinPoint.getSignature().toString();
                } else {
                    lockKey = noRepeatSubmit.prefix() + lockKey;
                }
            } else {
                lockKey = noRepeatSubmit.prefix() + joinPoint.getSignature().toString();
            }
        } catch (Exception e) {
            // 解析异常时使用默认锁键
            log.warn("解析防重复提交锁键异常，使用默认锁键: {}", e.getMessage());
            lockKey = noRepeatSubmit.prefix() + joinPoint.getSignature().toString();
        }

        // 获取锁并执行方法
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, noRepeatSubmit.timeout(), noRepeatSubmit.timeUnit());
            if (locked) {
                return joinPoint.proceed();
            } else {
                log.info("重复提交被拦截: {}", lockKey);
                throw new RRException(noRepeatSubmit.message());
            }
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
} 