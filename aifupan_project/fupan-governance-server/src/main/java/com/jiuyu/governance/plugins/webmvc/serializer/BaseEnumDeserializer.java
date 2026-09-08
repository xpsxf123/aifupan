package com.jiuyu.governance.plugins.webmvc.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.jiuyu.framework.shandard.BaseEnum;
import com.jiuyu.framework.util.EmptyUtil;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * 基枚举反序列化器
 *
 * @author HeHui
 * @date 2024-11-19 17:40
 */
@JacksonStdImpl
public class BaseEnumDeserializer extends JsonDeserializer<BaseEnum<?>> {


    /**
     * 自定义枚举类型反序列化方法
     * 该方法重写自父类，用于将JSON数据反序列化为指定的枚举类型
     *
     * @param jsonParser 当前的JSON解析器，用于访问JSON数据
     * @param deserializationContext 当前的反序列化上下文，用于处理反序列化过程中遇到的问题
     * @return 反序列化后的枚举对象，具体类型在运行时确定
     * @throws IOException 如果在反序列化过程中发生I/O错误
     */
    @Override
    public BaseEnum<?> deserialize(JsonParser jsonParser,
                                   DeserializationContext deserializationContext) throws IOException {
        // 获取当前值的实际类，用于确定目标枚举类型
        Object currentValue = jsonParser.currentValue();
        if (currentValue == null) {
            return null;
        }
        Class<?> targetClass = currentValue.getClass();
        // 获取当前JSON属性名，用于错误处理和日志记录
        String fieldName = jsonParser.currentName();
        // 获取当前JSON文本值，即枚举的名称
        String value = jsonParser.getText();
        // 调用业务逻辑方法，根据类、枚举值和字段名获取对应的枚举对象
        return this.getEnum(targetClass, value, fieldName);
    }


    /**
     * 根据值和字段名获取枚举对象
     * <pre>
     * 此方法用于在给定的目标类中，根据指定的值和字段名找到并返回对应的枚举对象
     * 它首先检查值是否为空，然后通过反射找到字段，并验证字段类型是否为枚举且继承自BaseEnum
     * 最后，它遍历枚举常量，比较枚举名称、值或描述与给定值是否匹配，如果匹配则返回对应的枚举对象
     * </pre>
     *
     * @param targetClass 目标类，即包含枚举字段的类
     * @param value       要匹配的值，用于找到对应的枚举对象
     * @param fieldName   字段名，指定要在哪个字段中寻找枚举对象
     *
     * @return 如果找到匹配的枚举对象，则返回该对象；否则返回null
     */
    private BaseEnum<?> getEnum(Class<?> targetClass, String value, String fieldName) {
        // 检查给定的值是否为空，如果为空则直接返回null
        if (EmptyUtil.isEmpty(value)) {
            return null;
        }
        // 使用反射找到指定字段名的字段
        Field field = ReflectionUtils.findField(targetClass, fieldName);
        // 如果字段不存在，则返回null
        if (Objects.isNull(field)) {
            return null;
        }
        // 获取字段的类型
        Class<?> fieldTypeClass = field.getType();
        // 检查字段类型是否为枚举且继承自BaseEnum，如果不是，则返回null
        if (!ClassUtils.isAssignable(BaseEnum.class, fieldTypeClass) || !fieldTypeClass.isEnum()) {
            return null;
        }
        // 获取枚举类型的枚举常量
        Object[] enumConstants = fieldTypeClass.getEnumConstants();
        // 遍历枚举常量，尝试找到匹配的枚举对象
        for (Object enumConstant : enumConstants) {
            // 如果枚举常量是BaseEnum类型，比较其值和描述与给定值是否匹配
            if (enumConstant instanceof BaseEnum<?> baseEnum) {
                if (baseEnum.getValue() instanceof Integer) {
                    if (Objects.equals(baseEnum.getValue() + "", value)) {
                        return baseEnum;
                    }
                }
                if (Objects.equals(baseEnum.getValue(), value) || Objects.equals(baseEnum.getDesc(), value)) {
                    return baseEnum;
                }
            }

            // 如果枚举常量是Enum类型，比较其名称与给定值是否匹配
            if (enumConstant instanceof Enum<?> enumConstantEnum) {
                if (Objects.equals(enumConstantEnum.name(), value)) {
                    if (enumConstant instanceof BaseEnum<?>) {
                        return (BaseEnum<?>) enumConstant;
                    }
                }
            }
        }
        // 如果没有找到匹配的枚举对象，返回null
        return null;
    }
}
