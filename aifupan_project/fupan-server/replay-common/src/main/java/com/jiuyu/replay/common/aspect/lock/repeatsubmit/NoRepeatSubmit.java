package com.jiuyu.replay.common.aspect.lock.repeatsubmit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 防止重复提交注解
 *
 * @author RayChou
 * @date 2025/6/6 18:27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRepeatSubmit {

    /**
     * 锁的前缀
     */
    String prefix() default "replay:lock:repeatSubmit:";

    /**
     * 锁的键值，支持SpEL表达式
     * 示例：
     * key = "#userId"
     * key = "#user.id"
     */
    String key() default "";

    /**
     * 锁的过期时间
     */
    long timeout() default 5;

    /**
     * 时间单位，默认秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 提示消息
     */
    String message() default "请勿重复提交，请稍后再试";
} 