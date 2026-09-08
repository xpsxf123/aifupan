package com.jiuyu.replay.common.utils;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 对象或者集合操作
 */
public class DataUtils {


    /**
     * 为对象的某个对象字段赋值
     *
     * @param targetObj    要设置值的对象
     * @param sourceId     条件id
     * @param setFieldName 设置值的字段名
     * @param values       数据集合
     * @param conditionsId 数据条件id
     * @param <T>
     * @param <V>
     */
    public static <T, V> void setFieldObject(T targetObj, String sourceId, String setFieldName, List<V> values, String conditionsId) {
        if (ObjectUtil.isEmpty(values)) return;
        Class<?> aClass = targetObj.getClass();
        // 判断对象中是否有该属性
        if (!ReflectUtil.hasField(aClass, sourceId)) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 判断对象中是否有该属性
        if (!ReflectUtil.hasField(aClass, setFieldName)) {
            throw new RuntimeException("对象中不存在该属性");
        }

        // 判断数据集合中是否有该属性
        if (values.stream().noneMatch(t -> ReflectUtil.hasField(t.getClass(), conditionsId))) {
            throw new RuntimeException("对象中不存在该属性");
        }

        // 判断数据类型是否一致
        Field field = ReflectUtil.getField(targetObj.getClass(), setFieldName);
        Class<?> type = field.getType();
        V firstValue = values.get(0);
        Class<?> valueType = firstValue.getClass();
        if (!type.isAssignableFrom(valueType)) {
            throw new RuntimeException("对象类型不匹配");
        }

        // 设置值
        for (V v : values) {
            Object fieldValue = ReflectUtil.getFieldValue(v, conditionsId);
            Object fieldValue1 = ReflectUtil.getFieldValue(targetObj, sourceId);
            if (ObjectUtil.equal(fieldValue, fieldValue1)) {
                ReflectUtil.setFieldValue(targetObj, setFieldName, v);
                break;
            }
        }
    }

    /**
     * 为集合中的对象赋值
     *
     * @param targetObj    设置值的对象集合
     * @param sourceId     条件id
     * @param setFieldName 设置值的字段名
     * @param values       数据集合
     * @param conditionsId 数据条件id
     * @param <T>
     * @param <V>
     */
    public static <T, V> void setFieldObject(List<T> targetObj, String sourceId, String setFieldName, List<V> values, String conditionsId) {
        if (ObjectUtil.isEmpty(targetObj)) return;
        if (ObjectUtil.isEmpty(values)) return;
        if (targetObj.stream().noneMatch(t -> ReflectUtil.hasField(t.getClass(), sourceId))) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 判断对象中是否有该属性
        if (targetObj.stream().noneMatch(t -> ReflectUtil.hasField(t.getClass(), setFieldName))) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 判断数据集合中是否有该属性
        if (values.stream().noneMatch(t -> ReflectUtil.hasField(t.getClass(), conditionsId))) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 判断数据类型是否一致
        Field field = ReflectUtil.getField(targetObj.get(0).getClass(), setFieldName);
        Class<?> type = field.getType();
        V firstValue = values.get(0);
        Class<?> valueType = firstValue.getClass();
        if (!type.isAssignableFrom(valueType)) {
            throw new RuntimeException("对象类型不匹配");
        }
        // 循环设置值
        for (T t : targetObj) {
            Object fieldValue = ReflectUtil.getFieldValue(t, sourceId);
            for (V v : values) {
                Object fieldValue1 = ReflectUtil.getFieldValue(v, conditionsId);
                if (ObjectUtil.equal(fieldValue, fieldValue1)) {
                    ReflectUtil.setFieldValue(t, setFieldName, v);
                    break;
                }
            }
        }
    }

    /**
     * 为对象的某个对象字段赋值
     * @param targetObj         设置值的对象集合
     * @param sourceId          条件id
     * @param setFieldName      设置值的字段名
     * @param values            数据集合
     * @param conditionsId      数据条件id
     * @param conditionsName    赋值的字段名
     * @param <T>
     * @param <V>
     */
    public static <T, V> void setFieldNameById(T targetObj, String sourceId, String setFieldName, List<V> values, String conditionsId, String conditionsName) {
        if (ObjectUtil.isEmpty(values)) return;
        Class<?> aClass = targetObj.getClass();
        // 判断对象中是否有该属性
        if (!ReflectUtil.hasField(aClass, sourceId)) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 判断对象中是否有该属性
        if (!ReflectUtil.hasField(aClass, setFieldName)) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 检查集合中的任意对象是否都不包含指定的属性
        if (values.stream().noneMatch(t -> ReflectUtil.hasField(t.getClass(), conditionsId))) {
            throw new RuntimeException("对象中不存在该属性");
        }

        if (values.stream().noneMatch(t -> ReflectUtil.hasField(t.getClass(), conditionsName))) {
            throw new RuntimeException("对象中不存在该属性");
        }

        // 判断数据类型是否一致
        Field field = ReflectUtil.getField(targetObj.getClass(), setFieldName);
        Class<?> type = field.getType();
        Class<?> valueType = ReflectUtil.getField(values.get(0).getClass(), conditionsName).getType();
        if (!type.isAssignableFrom(valueType)) {
            throw new RuntimeException("赋值对象类型不匹配");
        }

        // 设置值
        for (V v : values) {
            Object fieldValue = ReflectUtil.getFieldValue(v, conditionsId);
            Object fieldValue1 = ReflectUtil.getFieldValue(targetObj, sourceId);
            if (ObjectUtil.equal(fieldValue, fieldValue1)) {
                ReflectUtil.setFieldValue(targetObj, setFieldName, ReflectUtil.getFieldValue(v, conditionsName));
                break;
            }
        }
    }

    /**
     * 为集合中的对象赋值
     * @param targetObj         设置值的对象集合
     * @param sourceId          条件id
     * @param setFieldName      设置值的字段名
     * @param values            数据集合
     * @param conditionsId      数据条件id
     * @param conditionsName    赋值的字段名
     * @param <T>
     * @param <V>
     */
    public static <T, V> void setFieldNameById(List<T> targetObj, String sourceId, String setFieldName, List<V> values, String conditionsId, String conditionsName) {
        if (ObjectUtil.isEmpty(targetObj)) return;
        if (ObjectUtil.isEmpty(values)) return;
        // 判断对象中是否有该属性
        if (!ReflectUtil.hasField(targetObj.get(0).getClass(), sourceId)) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 判断对象中是否有该属性
        if (!ReflectUtil.hasField(targetObj.get(0).getClass(), setFieldName)) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 检查集合中的任意对象是否都不包含指定的属性
        if (values.stream().noneMatch(t -> ReflectUtil.hasField(t.getClass(), conditionsId))) {
            throw new RuntimeException("对象中不存在该属性");
        }

        if (values.stream().noneMatch(t -> ReflectUtil.hasField(t.getClass(), conditionsName))) {
            throw new RuntimeException("对象中不存在该属性");
        }

        // 判断数据类型是否一致
        Field field = ReflectUtil.getField(targetObj.get(0).getClass(), setFieldName);
        Class<?> type = field.getType();
        Class<?> valueType = ReflectUtil.getField(values.get(0).getClass(), conditionsName).getType();
        if (!type.isAssignableFrom(valueType)) {
            throw new RuntimeException("赋值对象类型不匹配");
        }
        // 设置值
        for (T t : targetObj){
            for (V v : values) {
                Object fieldValue = ReflectUtil.getFieldValue(v, conditionsId);
                Object fieldValue1 = ReflectUtil.getFieldValue(t, sourceId);
                if (ObjectUtil.equal(fieldValue, fieldValue1)) {
                    ReflectUtil.setFieldValue(t, setFieldName, ReflectUtil.getFieldValue(v, conditionsName));
                    break;
                }
            }
        }
    }

    /**
     * 为集合中的对象赋值
     * @param targetObj
     * @param sourceId
     * @param setFieldName
     * @param values
     * @param <T>
     * @param <V>
     */
    public static <T, V, L> void setFieldMap(List<T> targetObj, String sourceId, String setFieldName, Map<L, List<V>> values) {
        if (ObjectUtil.isEmpty(targetObj)) return;
        if (ObjectUtil.isEmpty(values)) return;
        // 判断对象中是否有该属性
        if (!ReflectUtil.hasField(targetObj.get(0).getClass(), sourceId)) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 判断对象中是否有该属性
        if (!ReflectUtil.hasField(targetObj.get(0).getClass(), setFieldName)) {
            throw new RuntimeException("对象中不存在该属性");
        }
        // 遍历目标对象集合，进行字段值的设置
        for (T t : targetObj){
            // 获取当前对象中指定字段的值
            Object value = ReflectUtil.getFieldValue(t, sourceId);
            // 获取待设置字段的类型
            // 从映射中获取与当前对象字段值关联的值列表
            List<V> vs = values.get(value);
            // 如果关联的值列表为空，则跳过当前对象
            if (ObjectUtil.isEmpty(vs)) continue;
            // 判断数据类型是否一致
            T t1 = targetObj.get(0);
            Field field = ReflectUtil.getField(t1.getClass(), setFieldName);
            Class<?> type = field.getType();
            Class<?> valueType = vs.getClass();
            if (!type.isAssignableFrom(valueType)) {
                throw new RuntimeException("赋值对象类型不匹配");
            }
            ReflectUtil.setFieldValue(t, setFieldName, vs);
        }
    }

}
