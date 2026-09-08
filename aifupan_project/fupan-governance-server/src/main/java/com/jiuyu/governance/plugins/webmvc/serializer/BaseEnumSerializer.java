package com.jiuyu.governance.plugins.webmvc.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.jiuyu.framework.shandard.BaseEnum;

import java.io.IOException;

/**
 * 枚举接口 BaseEnum 序列化器
 *
 * @author HeHui
 * @date 2024-11-19 17:40
 */
@JacksonStdImpl
public class BaseEnumSerializer extends JsonSerializer<BaseEnum> {


    @Override
    public void serialize(BaseEnum value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeObject(value.getValue());
    }
}
