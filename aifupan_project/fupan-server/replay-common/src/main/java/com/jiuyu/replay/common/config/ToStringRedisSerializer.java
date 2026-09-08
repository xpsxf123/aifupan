package com.jiuyu.replay.common.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class ToStringRedisSerializer implements RedisSerializer<Object>  {
    private final String charset;

    public ToStringRedisSerializer() {
        this(StandardCharsets.UTF_8.name());
    }

    public ToStringRedisSerializer(String charset) {
        this.charset = charset;
    }

    @Override
    public byte[] serialize(Object o) throws SerializationException {
        if (o == null) {
            return new byte[0];
        }

        try {
            // 将Long和Integer转成String
            if (o instanceof Integer || o instanceof Long) {
                return o.toString().getBytes(charset);
            }

            return ((String) o).getBytes(charset);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        // 反序列化时返回 String 类型
        try {
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
