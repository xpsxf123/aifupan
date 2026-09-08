package com.jiuyu.replay.api.interceptor;

import java.lang.annotation.*;

/**
 * 特性签名注解
 *
 * @author HeHui
 * @date 2026-03-23 19:14
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented // 文档中包含此注解
public @interface FeatureSignature {

    /**
     * 客户端appId。限定的是clientNumber 精确匹配
     *
     * @return 客户端标识，默认为空字符串。
     */
    String client() default "";
}
