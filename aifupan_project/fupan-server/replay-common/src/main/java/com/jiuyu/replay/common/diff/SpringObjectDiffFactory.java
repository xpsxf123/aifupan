package com.jiuyu.replay.common.diff;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ClassUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 基于 Spring 应用上下文的差异对比工厂实现。
 *
 * <p>该类通过查找 Spring 容器中注册的 {@link ObjectDiff} Bean 来构建差异对比器，
 * 并支持自动回退到默认的差分逻辑。适用于在 Spring 环境下灵活扩展和定制差分行为。</p>
 *
 * <h3>运行机制</h3>
 * <ol>
 *   <li>首先尝试从 Spring 上下文中查找符合泛型参数（sourceType -> targetType）的 {@link ObjectDiff} Bean。</li>
 *   <li>如果存在多个匹配项则返回 第一个，避免歧义。</li>
 *   <li>如果没有找到合适的 Bean，则使用默认的差分逻辑（基于反射转 Map 后逐字段比对）。</li>
 *   <li>结果会被缓存以提升后续调用性能。</li>
 * </ol>
 *
 * <h3>自定义实现方式</h3>
 * <p>可以通过以下方式自定义差分逻辑：</p>
 * <ul>
 *   <li>编写一个实现 {@link ObjectDiff} 接口的类，并指定泛型参数为目标类型和源类型。</li>
 *   <li>将该类注册为 Spring Bean，并确保泛型信息正确。</li>
 *   <li>框架会自动识别并优先使用这个自定义的差分器。</li>
 * </ul>
 * <p>
 * 示例：
 * <pre>{@code
 * @Component
 * public class MyCustomDiff implements ObjectDiff<MyTarget, MySource> {
 *     @Override
 *     public List<String> contrast(MyTarget target, MySource source) {
 *         // 自定义差分逻辑
 *     }
 * }
 * }</pre>
 *
 * @author HeHui
 * @date 2025-06-23 13:01
 */
@Component
public class SpringObjectDiffFactory implements ObjectDiffFactory {


    private final Map<String, ObjectDiff> diffMappers = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;

    public SpringObjectDiffFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取一个用于比较目标对象与源对象的差异对比器。
     *
     * <p>优先从 Spring 容器中查找已注册的差分器，未找到则使用默认实现。
     * 返回的差分器会被缓存，避免重复初始化。</p>
     *
     * @param targetClass 目标对象的类型（T）
     * @param sourceClass 源对象的类型（O）
     * @param <T>         目标对象类型
     * @param <O>         源对象类型
     *
     * @return 一个用于比较 T 和 O 类型对象的差异对比器
     *
     * @throws IllegalArgumentException 如果 targetClass 或 sourceClass 为 null
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T, O> ObjectDiff<T, O> getDiffMapper(Class<T> targetClass, Class<O> sourceClass) {
        if (targetClass == null || sourceClass == null) {
            throw new IllegalArgumentException("targetClass and sourceClass cannot be null");
        }
        String key = this.key(sourceClass, targetClass);
        return diffMappers.computeIfAbsent(key, k -> {
            ObjectDiff<T, O> objectDiff = this.findMapper(targetClass, sourceClass);
            if (objectDiff != null) {
                return objectDiff;
            }
            return defaultDiffMapper();
        });
    }


    /**
     * 构建用于缓存差分器的唯一键。
     *
     * <p>由源类型和目标类型的全限定类名拼接而成。</p>
     *
     * @param source 源对象类型
     * @param target 目标对象类型
     *
     * @return 缓存键
     */
    private String key(Class<?> source, Class<?> target) {
        return source.getName() + "__" + target.getName();
    }


    /**
     * 从 Spring 容器中查找符合给定类型的差分器。
     *
     * <p>利用 Spring 的泛型解析能力查找精确匹配的 Bean，
     * 避免因泛型擦除导致的误匹配。</p>
     *
     * @param targetType 目标对象类型
     * @param sourceType 源对象类型
     * @param <T>        目标对象类型
     * @param <O>        源对象类型
     *
     * @return 匹配的差分器，若无匹配或存在多个匹配则返回第一个
     */
    @SuppressWarnings("unchecked")
    private <T, O> ObjectDiff<T, O> findMapper(Class<T> targetType, Class<O> sourceType) {
        ResolvableType type = ResolvableType.forClassWithGenerics(ObjectDiff.class, sourceType, targetType);
        String[] beanNames = this.applicationContext.getBeanNamesForType(type);
        if (beanNames.length < 1) {
            return null;
        }
        return this.applicationContext.getBean(beanNames[0], ObjectDiff.class);
    }


    /**
     * 创建一个默认的差分器实现。
     *
     * <p>该实现通过将对象转换为 Map 形式进行字段级别的值比对，
     * 返回发生变更的字段名列表。</p>
     *
     * <p>适用于大多数简单 JavaBean 场景，但不处理嵌套对象、集合等复杂结构。</p>
     *
     * @param <T> 目标对象类型
     * @param <O> 源对象类型
     *
     * @return 默认实现的差分器
     */
    @SuppressWarnings("unchecked")
    private <T, O> ObjectDiff<T, O> defaultDiffMapper() {
        return (target, source) -> {
            // 检查对象是否为空，如果任一对象为空，则返回空Map
            if (target == null || source == null) {
                return List.of();
            }
            // 将原始对象转换为Map，以便进行属性值的比较
            Map<String, Object> originalMap = BeanUtil.beanToMap(source);
            // 如果原始Map为空，则将差异设置为空列表并返回
            if (CollUtil.isEmpty(originalMap)) {
                return List.of();
            }
            // 将当前对象转换为Map，以便与原始Map进行比较
            Map<String, Object> newMap = BeanUtil.beanToMap(target);

            return originalMap.entrySet().stream()
                .flatMap(entry -> {
                    // 比较当前属性在两个对象中的值是否相等
                    boolean equals = Objects.equals(newMap.get(entry.getKey()), entry.getValue());
                    if (equals) {
                        return Stream.empty();
                    }
                    Object value = entry.getValue();
                    // 如果属性值不为空且不是简单类型，则将属性值转换为字符串
                    if (value != null && !ClassUtil.isSimpleValueType(value.getClass())) {
                        value = BeanUtil.beanToMap(value);
                        if (newMap.get(entry.getKey()) != null) {
                            newMap.put(entry.getKey(), BeanUtil.beanToMap(newMap.get(entry.getKey())));
                        }
                    }
                    // 如果当前属性值是Map，并且新旧对象中的值不相等，则进一步比较Map内部的值
                    if (value instanceof Map originalValueMap && newMap.get(entry.getKey()) instanceof Map newValueMap) {
                        return originalValueMap.keySet().stream().filter(key -> !Objects.equals(originalValueMap.get(key), newValueMap.get(key)))
                            .map(key -> entry.getKey() + "." + key);
                    }
                    // 如果属性值不是Map或者Map内部的值也不同，则将该属性名添加到差异列表中
                    return Stream.of(entry.getKey());
                })
                // 收集并返回包含不同值的属性名和对应的新值的Map
                .toList();
        };
    }


    /**
     * 创建一个用于比较两个JSON对象差异的映射器
     * 该方法返回一个函数，该函数接受两个参数（目标和源），并返回它们之间的差异列表
     *
     * @param <T> 目标对象的类型
     * @param <O> 源对象的类型
     *
     * @return 一个用于比较两个对象差异的函数
     */
    private <T, O> ObjectDiff<T, O> jsonDiffMapper() {
        ObjectMapper mapper = new ObjectMapper();
        return (target, source) -> {
            try {
                // 将目标对象转换为JsonNode对象
                JsonNode after = mapper.readTree(mapper.writeValueAsString(target));
                // 将源对象转换为JsonNode对象
                JsonNode before = mapper.readTree(mapper.writeValueAsString(source));
                // 创建一个列表，用于存储差异
                List<String> diffs = new ArrayList<>();
                // 调用比较JSON的方法，计算差异
                this.compareJson(after, before, "", diffs);
                // 返回差异列表
                return diffs;
            } catch (JsonProcessingException e) {
                // 如果处理JSON时发生异常，返回一个空列表
                return List.of();
            }
        };
    }

    /**
     * 递归比较两个JSON节点的差异
     * 当节点为对象时，遍历每个字段并比较；当节点为其他类型时，直接比较节点值
     *
     * @param node1 第一个JSON节点
     * @param node2 第二个JSON节点
     * @param path  当前节点的路径，用于在差异列表中指示位置
     * @param diffs 存储差异的列表
     */
    private void compareJson(JsonNode node1, JsonNode node2, String path, List<String> diffs) {
        // 如果两个节点都是对象，则进一步比较它们的字段
        if (node1.isObject() && node2.isObject()) {
            // 遍历第一个对象的所有字段
            Iterator<Map.Entry<String, JsonNode>> fields1 = node1.fields();
            while (fields1.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields1.next();
                String fieldName = entry.getKey();
                JsonNode value1 = entry.getValue();
                JsonNode value2 = node2.get(fieldName);

                // 如果第二个对象中不存在当前字段，则记录差异
                if (value2 == null) {
                    diffs.add(path + fieldName);
                } else {
                    // 递归比较两个字段的值
                    compareJson(value1, value2, path + fieldName + ".", diffs);
                }
            }
            // 遍历第二个对象的所有字段，检查是否有第一个对象中不存在的字段
            Iterator<Map.Entry<String, JsonNode>> fields2 = node2.fields();
            while (fields2.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields2.next();
                String fieldName = entry.getKey();
                if (node1.get(fieldName) == null) {
                    diffs.add(path + fieldName);
                }
            }
        } else {
            // 如果两个节点不是对象，直接比较它们的值
            if (!node1.equals(node2)) {
                diffs.add(path);
            }
        }
    }
}
