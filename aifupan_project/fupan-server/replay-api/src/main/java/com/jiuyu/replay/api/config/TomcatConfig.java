package com.jiuyu.replay.api.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat服务器配置
 * 优化大请求处理能力和连接管理
 * @author RayChou
 * @date 2025/6/3 11:23
 */
@Configuration
public class TomcatConfig {

    /**
     * 自定义Tomcat连接器
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> {
            factory.addConnectorCustomizers(new TomcatConnectionCustomizer());
        };
    }

    /**
     * Tomcat连接器定制类
     */
    static class TomcatConnectionCustomizer implements TomcatConnectorCustomizer {
        @Override
        public void customize(Connector connector) {
            Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
            
            // 设置连接超时，单位毫秒
            protocol.setConnectionTimeout(10000);
            
            // 设置最大连接数
            protocol.setMaxConnections(10000);
            
            // 设置接受和处理的最大线程数
            protocol.setMaxThreads(200);

            // 初始线程数
            protocol.setMinSpareThreads(20);

            // 等待队列长度
            protocol.setAcceptCount(100);
            
            // 设置请求头缓冲区大小，单位字节
            protocol.setMaxHttpHeaderSize(10 * 1024);
            
            // 设置请求体缓冲区大小，单位字节
            protocol.setMaxSavePostSize(20 * 1024 * 1024);
            
            // 长连接设置
            protocol.setKeepAliveTimeout(60000);
            protocol.setMaxKeepAliveRequests(10000);
            
            // 启用压缩
            protocol.setCompression("on");
            protocol.setCompressionMinSize(2048);
            protocol.setCompressibleMimeType(
                    "text/html,text/xml,text/plain,text/css,text/javascript," +
                    "application/javascript,application/json,application/xml");
        }
    }
}