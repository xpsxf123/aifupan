package com.jiuyu.replay.api.filter;

import com.jiuyu.replay.api.utils.ProfileUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * 跨域过滤器
 * 根据不同环境提供不同的CORS配置
 * 使用Spring的CorsProcessor处理CORS，结合了灵活性和安全性
 *
 * @author RayChou
 * @date 2025/6/6 15:27
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 确保CORS过滤器在最高优先级执行
public class CorsFilter extends OncePerRequestFilter {

    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    private final ProfileUtil profileUtil;
    private final CorsProcessor corsProcessor = new DefaultCorsProcessor();

    public CorsFilter(ProfileUtil profileUtil) {
        this.profileUtil = profileUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 创建CORS配置
        CorsConfiguration config = createCorsConfiguration(request);

        // 处理CORS
        boolean isValid = corsProcessor.processRequest(config, request, response);

        // 修复：对于OPTIONS预检请求，我们仍然应该设置响应状态，然后返回
        if (!isValid) {
            // CORS处理失败，可能是非法的跨域请求
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // 对于预检请求，设置200状态码表示成功
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 继续处理请求
        filterChain.doFilter(request, response);
    }

    /**
     * CORS过滤器配置
     * 使用CorsFilter而不是WebMvcConfigurer，因为CorsFilter在安全过滤器链之前执行，
     * 可以确保CORS头被正确应用
     * <p>
     * 开发环境：允许所有来源（包括file://协议）
     * 生产环境：仅允许配置的特定域名
     */
    private CorsConfiguration createCorsConfiguration(HttpServletRequest request) {
        CorsConfiguration config = new CorsConfiguration();

        // 根据环境设置允许的来源
        if (profileUtil.isProd()) {
            // 生产环境：使用配置的特定域名
            String[] origins = allowedOrigins.split(",");

            // 如果使用了通配符"*"，则使用addAllowedOriginPattern
            if (origins.length == 1 && "*".equals(origins[0])) {
                config.addAllowedOriginPattern("*"); // 允许所有来源，包括file://协议
            } else {
                // 添加特定的域名
                Arrays.stream(origins).forEach(origin -> {
                    if (origin.contains("*")) {
                        config.addAllowedOriginPattern(origin); // 包含通配符的使用pattern
                    } else {
                        config.addAllowedOrigin(origin); // 精确域名
                    }
                });
            }
        } else {
            // 开发/测试环境：允许所有来源，包括file://协议
            config.addAllowedOriginPattern("*");
        }

        // 允许发送Cookie
        config.setAllowCredentials(true);

        // 允许的HTTP方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("PATCH");

        // 允许的请求头 - 根据实际请求头配置
        config.addAllowedHeader("Origin");
        config.addAllowedHeader("X-Requested-With");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Token");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Connection");
        config.addAllowedHeader("User-Agent");
        config.addAllowedHeader("Cookie");
        config.addAllowedHeader("Source");
        config.addAllowedHeader("X-App-Id");
        config.addAllowedHeader("X-Fingerprint");
        config.addAllowedHeader("X-Nonce");
        config.addAllowedHeader("X-RequestId");
        config.addAllowedHeader("X-Sign-Type");
        config.addAllowedHeader("X-Signature");
        config.addAllowedHeader("X-Timestamp");
        config.addAllowedHeader("Referer");
        config.addAllowedHeader("webversion");

        // 允许的响应头，使前端JavaScript可以访问
        config.addExposedHeader("Content-Disposition");
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Content-Length");
        config.addExposedHeader("Content-Range");
        config.addExposedHeader("X-Requested-With");
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Accept");
        config.addExposedHeader("Token");

        // 预检请求缓存时间
        config.setMaxAge(profileUtil.isProd() ? 7200L : 3600L);

        return config;
    }
}