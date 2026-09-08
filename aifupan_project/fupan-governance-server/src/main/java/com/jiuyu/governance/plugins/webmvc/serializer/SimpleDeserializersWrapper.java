package com.jiuyu.governance.plugins.webmvc.serializer;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.type.ClassKey;

/**
 * 反序列化器包装类（重写 Jackson 反序列化枚举方法，参阅：FasterXML/jackson-databind#2842）
 *
 * <p>
 * 默认处理：<br>
 * 1. Jackson 会先查找指定枚举类型对应的反序列化器（例如：GenderEnum 枚举类型，则是找 GenderEnum 枚举类型的对应反序列化器）；<br>
 * 2. 如果找不到则开始查找 Enum 类型（所有枚举父类）的反序列化器；<br>
 * 3. 如果都找不到则会采用默认的枚举反序列化器（它仅能根据枚举类型的 name、ordinal 来进行反序列化）。
 * </p>
 * <p>
 * 重写增强后：<br>
 * 1. 同默认 1；<br>
 * 2. 同默认 2；<br>
 * 3. 如果也找不到 Enum 类型（所有枚举父类）的反序列化器，开始查找指定枚举类型的接口的反序列化器（例如：GenderEnum 枚举类型，则是找它的接口 BaseEnum 的反序列化器）；<br>
 * 4. 同默认 3。
 * </p>
 *
 * @author HeHui
 * @date 2024-11-19 17:42
 */
public class SimpleDeserializersWrapper extends SimpleDeserializers {

    private static final long serialVersionUID = 2930635557731879991L;

    /**
     * 重写findEnumDeserializer方法以支持枚举类型的自定义反序列化
     *
     * @param type     枚举类的类型
     * @param config   反序列化配置
     * @param beanDesc 枚举类的描述信息
     *
     * @return 返回找到的枚举反序列化器，如果没有找到则返回null
     *
     * @throws JsonMappingException 如果反序列化过程中出现异常
     */
    @Override
    public JsonDeserializer<?> findEnumDeserializer(Class<?> type,
                                                    DeserializationConfig config,
                                                    BeanDescription beanDesc) throws JsonMappingException {
        // 调用父类方法尝试找到枚举类型的反序列化器
        JsonDeserializer<?> deser = super.findEnumDeserializer(type, config, beanDesc);
        // 如果找到了合适的反序列化器，则直接返回
        if (null != deser) {
            return deser;
        }
        // 重写增强：开始查找指定枚举类型的接口的反序列化器（例如：GenderEnum 枚举类型，则是找它的接口 BaseEnum 的反序列化器）
        for (Class<?> typeInterface : type.getInterfaces()) {
            // 尝试从映射中获取接口对应的反序列化器
            deser = this._classMappings.get(new ClassKey(typeInterface));
            // 如果找到合适的反序列化器，则返回
            if (null != deser) {
                return deser;
            }
        }
        // 如果没有找到合适的反序列化器，则返回null
        return null;
    }
}
