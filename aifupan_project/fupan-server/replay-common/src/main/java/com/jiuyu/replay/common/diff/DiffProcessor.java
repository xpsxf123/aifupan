package com.jiuyu.replay.common.diff;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;

import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import org.springframework.stereotype.Component;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import java.util.stream.Collectors;

/**
 * 差异处理器，用于比较两个对象之间的字段差异并记录变更日志。
 *
 * <p>{@code DiffProcessor} 是整个差分系统的核心执行入口，负责协调以下关键组件：</p>
 * <ul>
 *   <li>{@link ObjectDiffFactory}：获取具体的差分逻辑实现</li>
 *   <li>{@link ObjectDiff}：实际执行对象比对的函数式接口</li>
 *   <li>{@link DiffLogStorage}：将识别到的差异持久化或上报至日志/数据库等存储介质</li>
 * </ul>
 *
 * <h3>功能概述</h3>
 * <p>{@code DiffProcessor} 的核心流程如下：</p>
 * <ol>
 *   <li>接收目标对象（target）和源对象（source）</li>
 *   <li>通过工厂获取匹配的差分器（{@link ObjectDiff}）</li>
 *   <li>调用差分器获取变更字段列表</li>
 *   <li>若存在变更，则委托给存储接口保存变更记录</li>
 * </ol>
 *
 * <p>该类支持灵活扩展：</p>
 * <ul>
 *   <li>通过自定义实现 {@link ObjectDiff} 可以覆盖默认差分逻辑</li>
 *   <li>通过注入不同的 {@link DiffLogStorage} 实现可以定制差异记录的落地方式（如写入 DB、发送消息队列等）</li>
 * </ul>
 *
 * <h3>线程安全性</h3>
 * <p>{@code DiffProcessor} 本身是无状态的，其线程安全性取决于所依赖组件的实现。</p>
 *
 * @author HeHui
 * @date 2025-06-23 12:56
 */
@Component
@Slf4j
public class DiffProcessor {


    private final ObjectDiffFactory diffFactory;

    private final ApplicationContext applicationContext;

    private final Map<Business, BiFunction<Long, Long, Object>> beforeInfoQueryMap = new ConcurrentHashMap<>();

    /**
     * 创建一个新的差异处理器实例。
     *
     *
     * @param diffFactory 用于获取差分逻辑的工厂接口，不可为 null
     * @param applicationContext Spring 应用上下文，不可为 null
     * @param beforeSourceObjectProvider 用于提供差异前源的提供者
     */
    public DiffProcessor( ObjectDiffFactory diffFactory, ApplicationContext applicationContext, ObjectProvider<DiffBeforeSource> beforeSourceObjectProvider) {
        this.diffFactory = diffFactory;
        this.applicationContext = applicationContext;
        beforeSourceObjectProvider.orderedStream().forEach(beforeSource -> beforeInfoQueryMap.put(beforeSource.support(), beforeSource::querySource));
    }


    /**
     * 获取差异字段
     * 并且封装进List<Map<String, Object>>
     *    befor在前   after在后
     * @param business
     * @param target
     * @param source
     * @return
     * @param <T>
     * @param <O>
     */
    public <T, O> List<Map<String, Object>> compareGetMap(Business business, T target, O source) {
        List<String> diffFields = this.diffFields(business, target, source);
        log.info("[用户详情]-已获取差异字段-{}", JSONUtil.toJsonStr(diffFields));
        List<Map<String, Object>> mapList = new ArrayList<>();
        if (!diffFields.isEmpty()) {
            //原关注字段
            Map<String, Object> befFilter = this.filter(BeanUtil.beanToMap(source), diffFields);
            mapList.add(befFilter);
            //改后关注字段
            Map<String, Object> aftFilter = this.filter(BeanUtil.beanToMap(target), diffFields);
            mapList.add(aftFilter);
        }
        log.info("[用户详情]-已获取差异字段-回参打印{}", JSONUtil.toJsonStr(mapList));
        return mapList;
    }




    /**
     * 比较两个对象之间的差异字段
     *
     * @param business 业务类型，用于获取关注的字段名称
     * @param target 目标对象，用于比较差异
     * @param source 源对象，用于比较差异
     * @return 返回关注且发生变化的字段名称列表
     *
     * 此方法用于比较目标对象和源对象的字段差异，并根据业务类型过滤出关注的变更字段
     * 它首先检查输入对象的有效性，然后使用差异比较器进行字段对比，并最终返回关注的变更字段列表
     */
    public <T, O> List<String> diffFields(Business business, T target, O source) {
        // 如果目标对象或源对象为null，则不进行比较，直接返回false
        if (target == null || source == null) {
            return List.of();
        }
        // 如果业务类型为null，则抛出异常
        if (business == null) {
            throw new IllegalArgumentException("business is null");
        }
        // 根据目标对象和源对象的类类型获取差异比较器
        ObjectDiff<T, O> diff = diffFactory.getDiffMapper((Class<T>) target.getClass(), (Class<O>) source.getClass());
        // 使用差异比较器对比目标对象和源对象，获取变更的键集合
        List<String> changeKeys = diff.contrast(target, source);
        // 如果没有变更的键，则不保存差异记录，直接返回false
        if (CollUtil.isEmpty(changeKeys)) {
            return List.of();
        }
        // 根据业务类型的字段名称过滤出关注的变更键 todo 也可以做一个动态配置表 根据业务来配置需要关注的字段
        return business.getFieldNames().stream().filter(changeKeys::contains).toList();
    }


    /**
     * 过滤Map中的条目，只保留指定键的条目，并将null值替换为空字符串。
     *
     * @param map  要过滤的Map
     * @param keys 要保留的键列表
     *
     * @return 过滤后的Map
     */
    public Map<String, Object> filter(Map<String, Object> map, List<String> keys) {
        return map.entrySet().stream()
            .filter(entry -> keys.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() == null ? "" : entry.getValue()));
    }
}
