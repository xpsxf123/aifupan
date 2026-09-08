package com.jiuyu.replay.common.validated;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * @author RayChou
 * @version 1.0
 * @date 2019/6/14 11:55
 */

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = {EnumValueValidator.class})
public @interface EnumValue {
    //默认错误消息
    String message() default "必须为指定值";

    String[] strValues() default {};

    int[] intValues() default {};

    byte[] byteValues() default {};

    //分组
    Class<?>[] groups() default {};

    //负载
    Class<? extends Payload>[] payload() default {};

    //指定多个时使用
    @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        EnumValue[] value();
    }
}
