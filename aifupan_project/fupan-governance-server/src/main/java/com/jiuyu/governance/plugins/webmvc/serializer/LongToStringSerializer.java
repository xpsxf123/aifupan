package com.jiuyu.governance.plugins.webmvc.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * long转String 序列化
 *
 * @author HeHui
 * @date 2025-04-15 16:43
 */
public class LongToStringSerializer extends JsonSerializer<Long> {


    /**
     * 自定义 Long 类型序列化逻辑，处理大数值精度问题
     *
     * @param value 需要序列化的 Long 类型值，可能为 null
     * @param jsonGenerator JSON 生成器，用于写入序列化结果
     * @param serializerProvider 序列化器提供上下文信息
     * @throws IOException 当 JSON 写入发生 I/O 错误时抛出
     */
    @Override
    public void serialize(Long value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        // 处理 null 值情况
        if (value == null) {
            jsonGenerator.writeNull();
        }
        // 处理未超过 JavaScript 安全整数范围的值 (2^53)
        else if (value < 1000000000000000000L) { // 阈值设置为 1e18 (实际安全范围是 9e15)
            jsonGenerator.writeNumber(value);
        }
        // 处理大数值转换为字符串避免精度丢失
        else {
            jsonGenerator.writeString(value.toString());
        }
    }

}
