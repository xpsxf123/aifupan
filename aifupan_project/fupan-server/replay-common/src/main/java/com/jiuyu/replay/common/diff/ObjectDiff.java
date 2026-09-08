package com.jiuyu.replay.common.diff;



import java.util.List;


/**
 * 表示一种用于比较两个不同对象之间差异的函数式接口。
 *
 * <p>{@link ObjectDiff} 是一个函数式接口（Functional Interface），用于定义如何对比目标对象（T）与源对象（O）之间的字段差异。实现该接口的类应提供具体的差分逻辑，返回发生变化的字段名列表。</p>
 *
 * <h3>作用</h3>
 * <ul>
 *   <li>抽象出通用的对象差分行为，便于统一调用和扩展。</li>
 *   <li>支持泛型输入，允许对任意类型的对象进行差分比对。</li>
 *   <li>可被 Spring 容器管理，方便在运行时动态注入不同的差分策略。</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <p>要自定义差分逻辑，请实现该接口并指定泛型类型：</p>
 *
 * <p>如果注册为 Spring Bean，则会被 {@link SpringObjectDiffFactory} 自动识别并优先使用。</p>
 *
 * <h3>配置方式</h3>
 * <ol>
 *   <li><strong>自定义配置</strong>：
 *     <ul>
 *       <li>编写实现类并实现 {@link ObjectDiff} 接口。</li>
 *       <li>将其实现类注册为 Spring Bean（如使用 {@code @Component}）。</li>
 *       <li>确保泛型参数正确匹配目标类型和源类型。</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>应用场景</h3>
 * <ul>
 *   <li>业务变更记录（如用户信息修改日志）</li>
 *   <li>数据同步校验</li>
 *   <li>审计日志生成</li>
 *   <li>版本对比功能</li>
 * </ul>
 *
 * <h3>引用位置</h3>
 * <ul>
 *   <li>{@link ObjectDiffFactory#getDiffMapper(Class, Class)} - 获取具体实现</li>
 *   <li>{@link SpringObjectDiffFactory} - 默认实现和查找机制的核心类</li>
 * </ul>
 *
 * @author HeHui
 * @date 2025-06-23 11:45
 *
 * @param <T> 目标对象类型（通常是变更后的对象）
 * @param <O> 源对象类型（通常是变更前的对象）
 */
@FunctionalInterface
public interface ObjectDiff<T, O> {


    /**
     * 执行对象对比操作，返回发生变更的字段名列表。
     * <p>实现类应根据业务规则判断哪些字段发生了变化，并返回对应的字段名集合。</p>
     *
     * @param target 目标对象（通常是变更后或当前状态）
     * @param source 源对象（通常是变更前或原始状态）
     *
     * @return {@link List }<{@link String }> 发生变更的字段名列表
     */
    List<String> contrast(T target, O source);
}
