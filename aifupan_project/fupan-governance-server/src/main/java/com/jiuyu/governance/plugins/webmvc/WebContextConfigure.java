package com.jiuyu.governance.plugins.webmvc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.jiuyu.framework.constant.BasicConstant;
import com.jiuyu.framework.shandard.BaseEnum;
import com.jiuyu.governance.plugins.webmvc.serializer.*;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Arrays;

/**
 * web请求核心上下文配置
 *
 * @author HeHui
 * @date 2021-06-06 13:57
 */
@Configuration
@EnableConfigurationProperties(HttpClientPoolProperties.class)
public class WebContextConfigure {


    /**
     * 日期时间序列化及反序列化配置
     *
     * @return JavaTimeModule
     *
     * @since 1.0.0
     */
    @ConditionalOnProperty(prefix = "jiuyu.web.rest", name = "serializer", havingValue = "true", matchIfMissing = true)
    @Bean
    public Module customizedTimeModule() {
        // 针对大数值的序列化处理
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        // 针对时间类型：LocalDateTime 的序列化和反序列化处理
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(BasicConstant.NORM_DATETIME_PATTERN);
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        // 针对时间类型：LocalDate 的序列化和反序列化处理
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(BasicConstant.NORM_DATE_PATTERN);
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        // 针对时间类型：LocalTime 的序列化和反序列化处理（支持多种格式）
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(BasicConstant.NORM_TIME_PATTERN);
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        // 使用自定义反序列化器，支持 HH:mm 和 HH:mm:ss 两种格式
        javaTimeModule.addDeserializer(LocalTime.class, new FlexibleLocalTimeDeserializer());
        return javaTimeModule;
    }

    /**
     * 灵活的 LocalTime 反序列化器
     * 支持多种时间格式：HH:mm、HH:mm:ss
     *
     * @author HeHui
     * @date 2026-04-07
     */
    private static class FlexibleLocalTimeDeserializer extends JsonDeserializer<LocalTime> {
        
        private static final DateTimeFormatter FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");
        private static final DateTimeFormatter FORMATTER_HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        @Override
        public LocalTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String timeStr = parser.getText();
            if (timeStr == null || timeStr.isEmpty()) {
                return null;
            }
            
            // 尝试按 HH:mm:ss 格式解析
            try {
                return LocalTime.parse(timeStr, FORMATTER_HH_MM_SS);
            } catch (DateTimeParseException e) {
                // 如果失败，尝试按 HH:mm 格式解析
                try {
                    return LocalTime.parse(timeStr, FORMATTER_HH_MM);
                } catch (DateTimeParseException ex) {
                    throw context.wrongTokenException(parser, context.getContextualType(), 
                        null, String.format("无法解析时间格式 '%s'，支持的格式为：HH:mm 或 HH:mm:ss", timeStr));
                }
            }
        }
    }

    /**
     * 枚举序列化及反序列化配置
     *
     * @return SimpleModule
     *
     * @since 2.4.0
     */
    @Bean
    public Module customizedBaseEnumModule() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BaseEnum.class, new BaseEnumSerializer());
        SimpleDeserializersWrapper deserializers = new SimpleDeserializersWrapper();
        deserializers.addDeserializer(BaseEnum.class, new BaseEnumDeserializer());
        simpleModule.setDeserializers(deserializers);
        simpleModule.setSerializerModifier(new BaseEnumSerializerModifier());
        simpleModule.addSerializer(Long.class, new LongToStringSerializer());
        return simpleModule;
    }


    /**
     * 配置并创建一个格式化转换服务 bean
     * 该服务用于处理数据转换和格式化，特别是在 Web 请求和响应中
     *
     * @return {@link FormattingConversionService }
     */
    @Bean
    @ConditionalOnMissingBean
    public FormattingConversionService conversionService() {
        // 创建一个默认的格式化转换服务实例，启用缓存以提高性能
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService(true);

        // 注册日期格式化器
        DateFormatterRegistrar dateRegistrar = new DateFormatterRegistrar();
        dateRegistrar.setFormatter(new DateFormatter(BasicConstant.NORM_DATETIME_PATTERN));
        dateRegistrar.registerFormatters(conversionService);

        // 注册日期时间格式化器
        DateTimeFormatterRegistrar dateTimeRegistrar = new DateTimeFormatterRegistrar();
        dateTimeRegistrar.setDateFormatter(DateTimeFormatter.ofPattern(BasicConstant.NORM_DATE_PATTERN));
        dateTimeRegistrar.setTimeFormatter(DateTimeFormatter.ofPattern(BasicConstant.NORM_TIME_PATTERN));
        dateTimeRegistrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(BasicConstant.NORM_DATETIME_PATTERN));
        dateTimeRegistrar.setUseIsoFormat(false);
        dateTimeRegistrar.setDateStyle(FormatStyle.MEDIUM);
        dateTimeRegistrar.setDateTimeStyle(FormatStyle.FULL);
        dateTimeRegistrar.setTimeStyle(FormatStyle.MEDIUM);
        dateTimeRegistrar.registerFormatters(conversionService);
        return conversionService;
    }


    /**
     * 配置UndertowServletWebServerFactory的自定义处理器。
     * 该方法返回一个WebServerFactoryCustomizer实例，专门用于定制UndertowServletWebServerFactory的行为。
     * 特别地，它旨在配置WebSocket的支持，通过设置WebSocket的缓冲区和部署信息来启用和定制WebSocket功能。
     *
     * @return WebServerFactoryCustomizer<UndertowServletWebServerFactory> 一个定制器，用于调整UndertowServletWebServerFactory的配置。
     */
    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowCustomizationBean() {
        return factory -> {
            // 添加一个部署信息自定义器，用于配置WebSocket部署信息
            factory.addDeploymentInfoCustomizers(deploymentInfo -> {
                // 创建WebSocket部署信息对象
                WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
                // 配置WebSocket的缓冲区大小
                webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(false, 1024));
                // 将WebSocket部署信息对象添加为servlet上下文的属性，以启用WebSocket支持
                deploymentInfo.addServletContextAttribute("io.undertow.websockets.jsr.WebSocketDeploymentInfo", webSocketDeploymentInfo);
            });
        };
    }


    /**
     * 创建并配置一个HttpComponentsClientHttpRequestFactory bean
     * 用于创建HttpComponentsClientHttpRequestFactory对象，该对象用于创建HttpClient实例
     *
     * @param props HttpClientPoolProperties 实例，用于获取HttpClient连接池的配置参数
     *
     * @return HttpComponentsClientHttpRequestFactory 创建的HttpComponentsClientHttpRequestFactory对象
     */
    @ConditionalOnProperty(prefix = HttpClientPoolProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    @Bean
    public CloseableHttpClient httpComponentsClientHttpRequestFactory(HttpClientPoolProperties props) {
        // 1. Socket 配置：控制底层 TCP 读取行为
        SocketConfig socketConfig = SocketConfig.custom()
            .setSoTimeout(Timeout.of(props.getSocketTimeout()))
            .setTcpNoDelay(true) // 立即发送数据，禁用 Nagle 算法（生产环境推荐）
            .build();

        // 2. 连接配置：控制连接建立、生存周期
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.of(props.getConnectTimeout()))
            .setSocketTimeout(Timeout.of(props.getSocketTimeout()))
            .setTimeToLive(TimeValue.of(props.getTimeToLive()))
            .setValidateAfterInactivity(TimeValue.of(props.getValidateAfterInactivity()))
            .build();

        // 3. 连接管理器配置
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setTlsSocketStrategy(DefaultClientTlsStrategy.createDefault())
            .setMaxConnTotal(props.getMaxTotal())
            .setMaxConnPerRoute(props.getDefaultMaxPerRoute())
            .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
            .setConnPoolPolicy(PoolReusePolicy.LIFO)
            .setDefaultSocketConfig(socketConfig)
            .setDefaultConnectionConfig(connectionConfig)
            .build();

        // 4. 请求级别配置
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(Timeout.of(props.getConnectionRequestTimeout()))
            .setResponseTimeout(Timeout.of(props.getSocketTimeout())) // HttpClient5 推荐显式设置
            .build();

        // 5. 构建 HttpClient
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            // 优化：建议清理频率不要低于 10s，避免频繁争抢池锁
            .evictIdleConnections(TimeValue.of(props.getMaxIdleTime()))
            .evictExpiredConnections()
            .disableAutomaticRetries() // 生产环境建议通过 RestClient 的拦截器或 Resilience4j 控制重试
            .build();

    }


    /**
     * 跨域资源配置
     * <p>
     * 配置 CORS 过滤器，允许跨域请求，支持自定义配置的域名、方法、头部等。
     *
     * @return CorsFilter 跨域过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有域名跨域（生产环境建议配置具体域名）
        config.addAllowedOriginPattern("*");
        // 允许所有 HTTP 方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 允许所有头部信息
        config.addAllowedHeader("*");
        // 允许携带认证信息（cookies、authorization headers 等）
        config.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);
        // 暴露头部信息
        config.addExposedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用跨域配置
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}
