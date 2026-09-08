package com.jiuyu.replay.api.interceptor;

import com.jiuyu.framework.util.EmptyUtil;
import com.jiuyu.replay.api.config.SignatureConfig;
import com.jiuyu.replay.api.constant.SignatureConstants;
import com.jiuyu.replay.api.filter.CachedBodyHttpServletRequest;
import com.jiuyu.replay.api.utils.SignatureUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.SignatureException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 签名拦截器，应对与过滤器设计不当而无法实现的功能
 *
 * @author HeHui
 * @date 2026-03-23 14:39
 */
@Slf4j
@Component
public class SignatureInterceptor implements HandlerInterceptor {

    @Autowired
    private SignatureConfig signatureConfig;

    @Autowired
    private SignatureUtils signatureUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // requestId的过期时间（秒）
    private static final int REQUEST_ID_EXPIRE_SECONDS = 120; // 2分钟

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            FeatureSignature featureSignature = handlerMethod.getMethodAnnotation(FeatureSignature.class);
            if (featureSignature == null) {
                featureSignature = AnnotatedElementUtils.getMergedAnnotation(handlerMethod.getMethod().getDeclaringClass(), FeatureSignature.class);
            }
            if (featureSignature == null) {
                return true;
            }
            String requestURI = request.getRequestURI();
            // 获取请求头中的签名相关参数
            String appId = request.getHeader(SignatureConstants.HEADER_APP_ID);
            if (EmptyUtil.isNotEmpty(featureSignature.client()) && !Objects.equals(featureSignature.client(), appId)) {
                throw new SignatureException("签名验证失败: 客户端ID错误");
            }

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

            // 记录请求信息
            if (!"/".equals(requestURI)) {
                log.debug("[签名验证] 接收到签名请求: URI={}, Method={}, appId={}, timestamp={}, nonce={}, requestId={}, signType={}", requestURI, request.getMethod(), appId, timestamp, nonce, requestId, signType);
            }
            // 检查必要参数是否存在
            if (!StringUtils.hasText(appId) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signature) || !StringUtils.hasText(requestId)) {
                log.warn("[签名验证] 认证失败: 缺少签名参数, URI={}", requestURI);
                throw new SignatureException("缺少签名参数");
            }

            // 验证时间戳是否在允许的时间范围内
            long now = System.currentTimeMillis();
            long requestTimestamp = Long.parseLong(timestamp);
            if (Math.abs(now - requestTimestamp) > signatureConfig.getExpireTime()) {
                log.warn("[签名验证] 认证失败: 请求已过期, URI={}, 请求时间戳={}, 当前时间={}, 时间差={}ms", requestURI, requestTimestamp, now, Math.abs(now - requestTimestamp));
                throw new SignatureException("请求已过期，请勿更改系统时间！");
            }

            // 验证requestId是否重复 - 将requestId与appId绑定
            String requestIdKey = "replay:request_id:" + appId + ":" + requestId;
            Boolean isFirstRequest = redisTemplate.opsForValue().setIfAbsent(requestIdKey, "1", REQUEST_ID_EXPIRE_SECONDS, TimeUnit.SECONDS);
            if (isFirstRequest == null || !isFirstRequest) {
                log.warn("[签名验证] 认证失败: 请求ID已使用, URI={}, requestId={}", requestURI, requestId);
                throw new SignatureException("请求ID已使用");
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
                throw new SignatureException("签名验证失败");
            }

            // 签名验证通过，继续执行后续过滤器
            log.debug("[签名验证] 签名验证通过: URI={}, appId={}", requestURI, appId);
            return true;
        } else {
            return true;
        }
    }
}
