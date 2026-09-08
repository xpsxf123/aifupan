package com.jiuyu.replay.api.aspect;

import com.jiuyu.replay.api.annotation.UserLock;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@AllArgsConstructor
public class UserLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(userLock)")
    public Object around(ProceedingJoinPoint joinPoint, UserLock userLock) throws Throwable {
        String prefixKey = userLock.prefixKey();
        long expireTime = userLock.expireTime();
        long waitTime = userLock.waitTime();

        // 假设锁的 key 是由方法参数中的 userId 来构成的
        UserCacheVo user = GlobalObject.getLocalUser();
        String lockKey = prefixKey + user.getId();

        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 尝试获取锁，等待指定的时间，超过时间自动返回失败
            boolean locked = lock.tryLock(waitTime, expireTime, TimeUnit.SECONDS);

            if (!locked) {
                throw new RuntimeException("无法获取分布式锁，已超时");
            }

            // 执行目标方法
            return joinPoint.proceed();
        } finally {
            // 方法执行完毕后释放锁
            lock.unlock();
        }
    }
}