package com.jiuyu.replay.api.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson序列化配置
 * 统一配置：精度转换、时间格式、忽略未知字段
 *
 * @author RayChou
 * @date 2020/03/05
 */
@Configuration
public class JacksonSerializerConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 1. 精度转换：Long类型序列化为字符串，避免前端精度丢失
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

            // 2. LocalDateTime格式：yyyy-MM-dd HH:mm:ss
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer());
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer());

            // 3. LocalDate格式：yyyy-MM-dd
            builder.serializerByType(LocalDate.class, new LocalDateSerializer());
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer());

            // 4. 忽略未知字段，避免前端传递额外字段时报错
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }

    /**
     * LocalDateTime序列化器：yyyy-MM-dd HH:mm:ss
     */
    public static class LocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value != null) {
                gen.writeString(value.format(FORMATTER));
            }
        }
    }

    /**
     * LocalDateTime反序列化器：yyyy-MM-dd HH:mm:ss
     */
    public static class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext deserializationContext)
                throws IOException {
            String dateTimeStr = p.getValueAsString();
            if (dateTimeStr != null && !dateTimeStr.trim().isEmpty()) {
                return LocalDateTime.parse(dateTimeStr, FORMATTER);
            } else {
                return null;
            }
        }
    }

    /**
     * LocalDate序列化器：yyyy-MM-dd
     */
    public static class LocalDateSerializer extends JsonSerializer<LocalDate> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            if (value != null) {
                gen.writeString(value.format(FORMATTER));
            }
        }
    }

    /**
     * LocalDate反序列化器：yyyy-MM-dd
     */
    public static class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext deserializationContext)
                throws IOException {
            String dateStr = p.getValueAsString();
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                return LocalDate.parse(dateStr, FORMATTER);
            } else {
                return null;
            }
        }
    }
}
