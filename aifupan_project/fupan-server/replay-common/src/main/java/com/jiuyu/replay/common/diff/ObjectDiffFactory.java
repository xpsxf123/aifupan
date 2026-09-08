package com.jiuyu.replay.common.diff;


/**
 * 差异对比工厂接口，用于创建针对不同对象类型的差异对比器（{@link ObjectDiff}）。
 *
 * <p>该接口提供了一个统一的抽象，允许根据目标对象和源对象的类型动态获取对应的差异对比逻辑。
 * 实现类可以基于配置、注解或Spring上下文来决定使用哪种具体的差分策略。</p>
 *
 * @author HeHui
 * @date 2025-06-23 13:00
 */
public interface ObjectDiffFactory {


    /**
     * 获取一个针对特定目标类型和源类型的差异对比器。
     *
     * <p>该方法应返回一个实现了 {@link ObjectDiff} 接口的实例，用于比较两个对象之间的差异。
     * 如果没有找到匹配的实现，则可能返回默认的差分逻辑。</p>
     *
     * @param targetClass 目标对象的类型（T）
     * @param sourceClass 源对象的类型（O）
     * @return 一个用于比较 T 和 O 类型对象的差异对比器
     * @param <T> 目标对象类型
     * @param <O> 源对象类型
     */
    <T, O> ObjectDiff<T, O> getDiffMapper(Class<T> targetClass, Class<O> sourceClass);
}
