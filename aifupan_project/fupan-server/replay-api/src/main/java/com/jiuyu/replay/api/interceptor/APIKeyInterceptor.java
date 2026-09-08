
package com.jiuyu.replay.api.interceptor;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.common.open.APISecretProvide;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;
import java.util.Optional;

/**
 * 基于请求头 API-Key 的验证拦截器
 * <p>
 * 该拦截器用于验证客户端请求时提供的 API-Key，通过从请求头中获取密钥并与配置的密钥进行比对来实现身份验证。
 * 支持在方法或类级别使用 {@link APIKey} 注解来标识需要验证的接口。
 * </p>
 * <p>
 * 验证流程：
 * <ol>
 *     <li>检查处理的方法或类是否标注了 {@link APIKey} 注解</li>
 *     <li>从请求头中获取指定名称的密钥值</li>
 *     <li>从 {@link APISecretProvide} 中获取配置的密钥</li>
 *     <li>比对请求密钥与配置密钥是否一致</li>
 * </ol>
 * </p>
 *
 * @author HeHui
 * @date 2026-02-24 23:13
 * @see APIKey
 * @see APISecretProvide
 * @see HandlerInterceptor
 */
public class APIKeyInterceptor implements HandlerInterceptor {

    private final Logger log = LoggerFactory.getLogger(APIKeyInterceptor.class);

    /**
     * 密钥提供接口，用于获取应用对应的密钥配置
     */
    private final APISecretProvide secretProvide;

    /**
     * 构造方法
     *
     * @param secretProvide 密钥提供者，用于获取各应用的 API-Key 密钥配置
     */
    public APIKeyInterceptor(APISecretProvide secretProvide) {
        this.secretProvide = secretProvide;
    }

    /**
     * 在请求处理之前进行预处理，执行 API-Key 验证逻辑
     * <p>
     * 验证逻辑：
     * <ol>
     *     <li>如果处理器不是 {@link HandlerMethod} 类型，则跳过验证，直接放行</li>
     *     <li>检查方法或类是否标注了 {@link APIKey} 注解，未标注则跳过验证</li>
     *     <li>从请求头中获取指定名称的密钥值，为空则返回 403 错误</li>
     *     <li>从密钥存储器中获取配置的密钥，未配置则返回 403 警告</li>
     *     <li>比对请求密钥与配置密钥，不匹配则返回 403 错误</li>
     *     <li>验证通过，允许请求继续处理</li>
     * </ol>
     * </p>
     *
     * @param request  HTTP 请求对象，用于获取请求头和客户端信息
     * @param response HTTP 响应对象，用于返回错误响应
     * @param handler  当前请求的处理器，可能是 {@link HandlerMethod} 或其他类型
     * @return {@code true} 表示验证通过，请求可以继续处理；{@code false} 表示验证失败，请求被终止
     * @throws Exception 当写入错误响应时可能抛出异常
     * @see HandlerInterceptor#preHandle(HttpServletRequest, HttpServletResponse, Object)
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            // 从方法注解获取 API-Key 配置
            APIKey apiKey = handlerMethod.getMethodAnnotation(APIKey.class);
            if (apiKey == null) {
                // 尝试从类注解获取 API-Key 配置
                apiKey = AnnotatedElementUtils.getMergedAnnotation(handlerMethod.getMethod().getDeclaringClass(), APIKey.class);
            }
            if (apiKey == null) {
                // 未标注 API-Key 注解，跳过验证
                return true;
            }
            String clientAppId = apiKey.appId();
            if (EmptyUtil.isEmpty(clientAppId)) {
                clientAppId = request.getHeader(apiKey.clientName());
                if (EmptyUtil.isEmpty(clientAppId)) {
                    this.errorResponse(response);
                    log.error("[鉴权] oauth client app {}, header {} empty, ip {}", clientAppId, apiKey.clientName(), this.getRemoteIp(request));
                    return false;
                }
            }

            // 从请求头中获取密钥值
            String clientSecret = request.getHeader(apiKey.headerName());
            if (EmptyUtil.isEmpty(clientSecret)) {
                // 请求头中密钥值为空，返回错误响应
                this.errorResponse(response);
                log.error("[鉴权] oauth client app {}, header {} empty, ip {}", clientAppId, apiKey.headerName(), this.getRemoteIp(request));
                return false;
            }
            // 从密钥存储器获取配置的密钥
            Optional<String> secretOptional = secretProvide.getSecret(clientAppId);
            if (secretOptional.isEmpty()) {
                // 服务端未配置该应用的密钥，返回警告
                this.errorResponse(response);
                log.warn("[鉴权] oauth client app {}, secret not configuration !!!, ip {}", clientAppId, this.getRemoteIp(request));
                return false;
            }
            String secret = secretOptional.get();
            // 比对请求密钥与配置密钥
            if (!Objects.equals(clientSecret, secret)) {
                // 密钥不匹配，返回错误响应
                this.errorResponse(response);
                log.warn("[鉴权] oauth client app {}, header {} secret unable match, ip {}, client request secret: {}", clientAppId, apiKey.headerName(), this.getRemoteIp(request), clientSecret);
                return false;
            }
            // 验证通过
            return true;
        } else {
            return true;
        }
    }

    /**
     * 生成错误响应，返回 403 禁止访问的 JSON 格式错误信息
     * <p>
     * 响应状态码设置为 200（OK），但响应体中包含 403 错误码和错误消息，
     * 这是为了兼容某些前端框架对 HTTP 状态码的处理方式。
     * </p>
     *
     * @param response HTTP 响应对象，用于设置响应状态码、内容类型和响应体
     * @throws Exception 当写入响应内容失败时抛出异常
     */
    private void errorResponse(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"警告：请提供有效的api-key\"}");
        response.flushBuffer();
    }

    /**
     * 获取客户端真实 IP 地址
     * <p>
     * 按照优先级从以下来源获取 IP：
     * <ol>
     *     <li>{@code X-Forwarded-For} 请求头 - 适用于经过反向代理或负载均衡的场景</li>
     *     <li>{@code X-Real-IP} 请求头 - 作为备选代理头</li>
     *     <li>{@code RemoteAddr} - 直接使用 TCP 连接的远程地址</li>
     * </ol>
     * </p>
     *
     * @param request HTTP 请求对象，用于获取请求头和连接信息
     * @return 客户端 IP 地址字符串，如果所有来源都为空则返回 {@code null}
     */
    private String getRemoteIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
