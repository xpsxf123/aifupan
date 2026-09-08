package com.jiuyu.replay.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标记需要加锁的用户操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantLock {
    // 锁的前缀，用户唯一标识（例如：userId）
    String prefixKey() default "tenant_lock:";

    // 锁的过期时间，单位秒
    long expireTime() default 60; // 默认 60 秒

    // 获取锁的等待时间，单位秒
    long waitTime() default 10; // 默认 10 秒
}