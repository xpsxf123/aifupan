package com.jiuyu.replay.common.open;

import java.lang.annotation.*;

/**
 * 基于请求头api-key的注解验证
 *
 * @author HeHui
 * @date 2026-03-03 01:20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
public @interface APIKey {

    /**
     * 请求头名称
     *
     * @return 请求头名称
     */
    String headerName() default "api-key";


    /**
     * 客户端名称 用于从请求头中获取客户端ID
     *
     * @return 客户端名称
     */
    String clientName() default "x-jiuyu-client-id";


    /**
     * 限制指定应用id进入
     *
     * @return 应用id
     */
    String appId() default "";
}
