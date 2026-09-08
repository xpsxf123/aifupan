package com.jiuyu.governance.plugins.webmvc.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jiuyu.framework.shandard.BaseEnum;

import java.io.IOException;

/**
 * 字典序列化
 *
 * @author HeHui
 * @date 2025-01-04 23:44
 */
public class DictSerializer extends JsonSerializer<Object> {

    private final Class<? extends BaseEnum<?>> enumClass;

    public DictSerializer(final Class<? extends BaseEnum<?>> enumClass) {
        this.enumClass = enumClass;
    }

    /**
     * Method that can be called to ask implementation to serialize
     * values of type this serializer handles.
     *
     * @param value       Value to serialize; can <b>not</b> be null.
     * @param gen         Generator used to output resulting Json content
     * @param serializers Provider that can be used to get serializers for
     *                    serializing Objects value contains, if any.
     */
    @Override
    public void serialize(final Object value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        if (value == null) {
            return;
        }
        if (value instanceof BaseEnum) {
            gen.writeString(((BaseEnum<?>) value).getDesc());
            return;
        }
        if (enumClass == null) {
            return;
        }
        BaseEnum<?>[] enumConstants = enumClass.getEnumConstants();
        for (BaseEnum<?> baseEnum : enumConstants) {
            if (baseEnum.getValue().equals(value)) {
                gen.writeString(baseEnum.getDesc());
                return;
            }
        }
    }
}
