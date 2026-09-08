package com.jiuyu.governance.plugins.webmvc.serializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.jiuyu.framework.shandard.BaseEnum;

import java.lang.annotation.*;

/**
 * 枚举描述
 *
 * @author HeHui
 * @date 2024/12/04
 */
@JsonSerialize
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface EnumDesc {

    /**
     * 枚举类
     *
     * @return {@link Class }<{@link ? } {@link extends } {@link BaseEnum }<{@link ? }>>
     */
    Class<? extends BaseEnum<?>> value();

    /**
     * 字段名后缀
     *
     * @return {@link String }
     */
    String field() default "Desc";
}
