package com.jiuyu.replay.common.annotation;

import org.springframework.aot.hint.annotation.Reflective;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标记需要加锁的操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Reflective
public @interface SingletonLock {

    // 锁的前缀
    String prefixKey() default "singleton_lock:";

    // 锁的键值，用于唯一标识锁 spring 的el表达式
    // 示例：
    // key = "#userId"
    // key = "#object.fieldName"
    // key = "#object.fieldName"
    String key() default "";

    // 锁的过期时间，单位秒
    long expireTime() default 60; // 默认 60 秒

    // 获取锁的等待时间，单位秒
    long waitTime() default 10; // 默认 10 秒

}
