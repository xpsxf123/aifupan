package com.jiuyu.replay.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuyu.replay.api.config.SignatureConfig;
import com.jiuyu.replay.api.constant.SignatureConstants;
import com.jiuyu.replay.api.utils.GetIPUtils;
import com.jiuyu.replay.api.utils.SignatureUtils;
import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.generic.vo.common.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 接口签名验证过滤器
 *
 * @author RayChou
 * @date 2025/6/9 14:28
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // 确保在CORS过滤器之后执行
public class SignatureAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SignatureAuthFilter.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // requestId的过期时间（秒）
    private static final int REQUEST_ID_EXPIRE_SECONDS = 120; // 2分钟

    @Autowired
    private SignatureConfig signatureConfig;

    @Autowired
    private SignatureUtils signatureUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 初始化请求上下文
        RequestContext.init();
        // 设置指纹信息
        RequestContext.setFingerprint(request.getHeader(SignatureConstants.HEADER_FINGERPRINT));
        // 对于OPTIONS预检请求，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            log.debug("[签名验证] OPTIONS预检请求，跳过签名验证: URI={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        if (!signatureConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        String requestURI = request.getRequestURI();
        /*// 检查是否需要跳过签名验证
        if (isExcluded(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }*/
        // TODO 兼容现有接口不进行全部签名验证进行反向逻辑把需要签名的接口加入到yml配置文件signature.include-paths列表中
        if (!isInclude(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            // 获取请求头中的签名相关参数
            String appId = request.getHeader(SignatureConstants.HEADER_APP_ID);
            String timestamp = request.getHeader(SignatureConstants.HEADER_TIMESTAMP);
            String nonce = request.getHeader(SignatureConstants.HEADER_NONCE);
            String signature = request.getHeader(SignatureConstants.HEADER_SIGNATURE);
            String fingerprint = request.getHeader(SignatureConstants.HEADER_FINGERPRINT);
            String requestId = request.getHeader(SignatureConstants.HEADER_REQUEST_ID);
            String signType = request.getHeader(SignatureConstants.HEADER_SIGN_TYPE);

            // 如果未指定签名类型，使用默认签名类型
            if (!StringUtils.hasText(signType)) {
                signType = SignatureConstants.DEFAULT_SIGN_TYPE;
            }

            // 存储请求信息到RequestContext
            RequestContext.setAppId(appId);
            RequestContext.setTimestamp(timestamp);
            RequestContext.setNonce(nonce);
            RequestContext.setRequestId(requestId);
            RequestContext.setSignType(signType);
            // 获取客户端IP并存储
            RequestContext.setIpAddress(GetIPUtils.getIpAddr(request));
            RequestContext.setRequestURI(requestURI);

            // 记录请求信息
            if (!"/".equals(requestURI)) {
                log.debug("[签名验证] 接收到签名请求: URI={}, Method={}, appId={}, timestamp={}, nonce={}, requestId={}, signType={}", requestURI, request.getMethod(), appId, timestamp, nonce, requestId, signType);
            }
            // 检查必要参数是否存在
            if (!StringUtils.hasText(appId) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature) || !StringUtils.hasText(requestId)) {
                log.warn("[签名验证] 认证失败: 缺少签名参数, URI={}", requestURI);
                writeErrorResponse(response, "缺少签名参数");
                return;
            }

            // 验证时间戳是否在允许的时间范围内
            long now = System.currentTimeMillis();
            long requestTimestamp = Long.parseLong(timestamp);
            if (Math.abs(now - requestTimestamp) > signatureConfig.getExpireTime()) {
                log.warn("[签名验证] 认证失败: 请求已过期, URI={}, 请求时间戳={}, 当前时间={}, 时间差={}ms", requestURI, requestTimestamp, now, Math.abs(now - requestTimestamp));
                writeErrorResponse(response, "请求已过期，请勿更改系统时间！");
                return;
            }

            // 验证requestId是否重复 - 将requestId与appId绑定
            String requestIdKey = "replay:request_id:" + appId + ":" + requestId;
            Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(requestIdKey, "1", REQUEST_ID_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (isFirstRequest == null || !isFirstRequest) {
                log.warn("[签名验证] 认证失败: 请求ID已使用, URI={}, requestId={}", requestURI, requestId);
                writeErrorResponse(response, "请求ID已使用");
                return;
            }

            // 包装请求，以便可以多次读取请求体
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

            // 根据请求类型获取需要签名的内容
            String contentToSign = cachedRequest.getContentToSign();

            // 记录请求信息
            String method = cachedRequest.getMethod();
            String contentType = cachedRequest.getContentType();
            log.debug("[签名验证] 请求方法: {}, 内容类型: {}", method, contentType);

            if (StringUtils.hasText(contentToSign)) {
                log.debug("[签名验证] 待签名内容: {}", contentToSign);
            } else {
                log.debug("[签名验证] 待签名内容为空");
            }

            // 验证签名
            boolean isValid = signatureUtils.verifySignature(appId, timestamp, nonce, fingerprint, requestId, contentToSign, signature, signType);

            if (!isValid) {
                log.warn("[签名验证] 认证失败: 签名验证失败, URI={}, appId={}, signType={}", requestURI, appId, signType);
                writeErrorResponse(response, "签名验证失败");
                return;
            }

            // 签名验证通过，继续执行后续过滤器
            log.debug("[签名验证] 签名验证通过: URI={}, appId={}", requestURI, appId);
            filterChain.doFilter(cachedRequest, response);
        } catch (Exception e) {
            log.error("[签名验证] 签名验证异常: URI={}, 异常类型={}, 异常信息={}", request.getRequestURI(), e.getClass().getName(), e.getMessage(), e);
            writeErrorResponse(response, "签名验证异常: " + e.getMessage());
        }
    }

    /**
     * 判断请求路径是否需要签名验证
     *
     * @param requestURI
     * @return
     */
    private boolean isInclude(String requestURI) {
        for (String pattern : signatureConfig.getIncludePaths()) {
            if (pathMatcher.match(pattern, requestURI)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断请求路径是否需要跳过签名验证
     */
    private boolean isExcluded(String requestURI) {
        for (String pattern : signatureConfig.getExcludePaths()) {
            if (pathMatcher.match(pattern, requestURI)) {
                log.debug("[签名验证] 跳过签名验证: URI={}, 匹配排除路径={}", requestURI, pattern);
                return true;
            }
        }
        return false;
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        // 删除CORS响应头配置，由CorsFilter统一处理

        // 设置正确的内容类型和字符编码
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.OK.value());
        R<Object> result = R.error(HttpStatus.UNAUTHORIZED.value(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
} 