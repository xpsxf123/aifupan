package com.jiuyu.replay.common.utils;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/4 15:27
 */
public class UpdateWrapperUtils {

    /**
     * 简化版本 - 生成更新全部字段为null的Wrapper
     *
     * @param entity 实体类实例
     * @param <T>    实体类型
     * @return LambdaUpdateWrapper
     */
    public static <T> UpdateWrapper<T> createFullNullUpdateWrapperSimple(T entity) {
        if (entity == null) {
            return new UpdateWrapper<T>();
        }

        UpdateWrapper<T> wrapper = new UpdateWrapper<>();
        Class<?> entityClass = entity.getClass();

        // 设置主键条件
        setPrimaryKeySimple(wrapper, entity, entityClass);

        // 设置所有字段为null
        setAllFieldsNullSimple(wrapper, entityClass, entity);

        return wrapper;
    }

    /**
     * 简化版设置主键条件
     */
    private static <T> void setPrimaryKeySimple(UpdateWrapper<T> wrapper, T entity, Class<?> entityClass) {
        try {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
            if (tableInfo != null && tableInfo.getKeyProperty() != null) {
                String keyProperty = tableInfo.getKeyProperty();
                Field keyField = entityClass.getDeclaredField(keyProperty);
                keyField.setAccessible(true);
                Object keyValue = keyField.get(entity);

                if (keyValue != null) {
                    // 直接使用字段名设置条件（需要确保字段名正确）
                    wrapper.eq(tableInfo.getKeyProperty(), keyValue);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("设置主键条件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简化版设置所有字段为null
     */
    private static <T> void setAllFieldsNullSimple(UpdateWrapper<T> wrapper, Class<?> entityClass, T entity) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo != null && ObjectUtil.isNotEmpty(tableInfo.getFieldList())) {
            List<TableFieldInfo> fieldList = tableInfo.getFieldList();

            for (TableFieldInfo info : fieldList) {

                Object value = tableInfo.getPropertyValue(entity, info.getProperty());

                try {
                    wrapper.set(info.getColumn(), value);
                } catch (Exception e) {
                    System.out.println("跳过字段: " + info.getColumn());
                }
            }


        }
    }

    /**
     * 判断字段是否是主键
     */
    private static boolean isPrimaryKey(Field field, Class<?> entityClass) {
        try {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
            if (tableInfo != null && tableInfo.getKeyProperty() != null) {
                return field.getName().equals(tableInfo.getKeyProperty());
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return false;
    }

    /**
     * 判断字段是否有@TableField(exist = false)注解
     */
    private static boolean isTableFieldNotExist(Field field) {
        TableField tableField = field.getAnnotation(TableField.class);
        return tableField != null && !tableField.exist();
    }


}
