package com.jiuyu.replay.common.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用对象转换工具类
 * 用于Entity转Bo、Bo转Dto或Bo转Vo等场景
 *
 * @author RayChou
 * @date 2025/6/4
 */
public class BeanConvertUtils {

    /**
     * 单个对象转换
     *
     * @param source 源对象
     * @param targetClass 目标类型
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("对象转换失败", e);
        }
    }

    /**
     * 单个对象转换，带自定义转换逻辑
     *
     * @param source 源对象
     * @param targetClass 目标类型
     * @param customizer 自定义转换逻辑
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass, Function<T, T> customizer) {
        if (source == null) {
            return null;
        }
        T target = convert(source, targetClass);
        return customizer.apply(target);
    }

    /**
     * 列表对象转换
     *
     * @param sourceList 源对象列表
     * @param targetClass 目标类型
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> convert(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 列表对象转换，带自定义转换逻辑
     *
     * @param sourceList 源对象列表
     * @param targetClass 目标类型
     * @param customizer 自定义转换逻辑
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass, Function<T, T> customizer) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> {
                    T target = convert(source, targetClass);
                    return customizer.apply(target);
                })
                .collect(Collectors.toList());
    }

    /**
     * 分页对象转换
     *
     * @param sourcePage 源分页对象
     * @param targetClass 目标类型
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的分页对象
     */
    public static <S, T> List<T> convertPage(List<S> sourcePage, Class<T> targetClass) {
        return convertList(sourcePage, targetClass);
    }

    /**
     * 分页对象转换，带自定义转换逻辑
     *
     * @param sourcePage 源分页对象
     * @param targetClass 目标类型
     * @param customizer 自定义转换逻辑
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的分页对象
     */
    public static <S, T> List<T> convertPage(List<S> sourcePage, Class<T> targetClass, Function<T, T> customizer) {
        return convertList(sourcePage, targetClass, customizer);
    }

    /**
     * 批量转换对象列表
     *
     * @param sourceList 源对象列表
     * @param converter 转换器
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Function<S, T> converter) {
        if (CollectionUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            targetList.add(converter.apply(source));
        }
        return targetList;
    }
}