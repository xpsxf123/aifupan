package com.jiuyu.replay.third.tablestore.annotation;

import com.alicloud.openservices.tablestore.model.ColumnType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表格存储，范围查询注解
 */
@Target(ElementType.FIELD) // 注解作用于字段
@Retention(RetentionPolicy.RUNTIME) // 注解在运行时可用
public @interface TableStoreRangeAnnotation {

    /**
     * 字段名称
     * @return
     */
    String name();

    /**
     * 字段类型
     * @return
     */
    ColumnType type() default ColumnType.STRING;

    /**
     * 是否是索引字段
     */
    boolean isIndex() default false;

    /**
     * 是否是表格存储字段
     * @return
     */
    boolean isHasTableStore() default true;
}
