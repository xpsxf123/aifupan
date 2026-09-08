package com.jiuyu.replay.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomRedissonLock {

    /**
     * 锁的key（支持Spring EL表达式，不需要传#{}边界符）
     * @return
     */
    String key() default "";

    /**
     * 等待锁的最大时间（秒）
     * @return
     */
    int waitTime() default 30;

    /**
     * 锁的自动释放时间 (秒)
     * @return
     */
    int leaseTime() default 60;
}
