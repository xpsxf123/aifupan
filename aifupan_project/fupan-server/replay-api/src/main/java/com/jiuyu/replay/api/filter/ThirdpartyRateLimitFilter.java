package com.jiuyu.replay.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiuyu.replay.api.constant.SignatureConstants;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 第三方接口限流过滤器
 * 基于Redis实现按appId维度的双重限流：每秒10次 + 每分钟60次
 *
 * @author System
 * @date 2026-04-09
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 11) // 在签名验证过滤器之后执行
public class ThirdpartyRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ThirdpartyRateLimitFilter.class);

    private static final String THIRDPARTY_PATH_PATTERN = "/replay/openapi/thirdparty/**";

    /**
     * 每秒最高请求次数
     */
    private static final long MAX_REQUESTS_PER_SECOND = 10;

    /**
     * 每分钟最高请求次数
     */
    private static final long MAX_REQUESTS_PER_MINUTE = 60;

    /**
     * Redis Key前缀
     */
    private static final String RATE_LIMIT_PREFIX = "rate_limit:thirdparty:";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 只对第三方接口路径生效
        if (!pathMatcher.match(THIRDPARTY_PATH_PATTERN, requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头获取appId
        String appId = request.getHeader(SignatureConstants.HEADER_APP_ID);
        if (!StringUtils.hasText(appId)) {
            appId = "unknown";
        }

        // 秒级限流
        long currentSecond = System.currentTimeMillis() / 1000;
        String secKey = RATE_LIMIT_PREFIX + appId + ":sec:" + currentSecond;
        Long secCount = stringRedisTemplate.opsForValue().increment(secKey);
        if (secCount != null && secCount == 1) {
            stringRedisTemplate.expire(secKey, 2, TimeUnit.SECONDS);
        }
        if (secCount != null && secCount > MAX_REQUESTS_PER_SECOND) {
            log.warn("第三方接口限流触发（秒级）: appId={}, count={}", appId, secCount);
            writeRateLimitResponse(response, "请求过于频繁，请稍后再试（每秒最多" + MAX_REQUESTS_PER_SECOND + "次）");
            return;
        }

        // 分钟级限流
        long currentMinute = System.currentTimeMillis() / 60000;
        String minKey = RATE_LIMIT_PREFIX + appId + ":min:" + currentMinute;
        Long minCount = stringRedisTemplate.opsForValue().increment(minKey);
        if (minCount != null && minCount == 1) {
            stringRedisTemplate.expire(minKey, 62, TimeUnit.SECONDS);
        }
        if (minCount != null && minCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("第三方接口限流触发（分钟级）: appId={}, count={}", appId, minCount);
            writeRateLimitResponse(response, "请求过于频繁，请稍后再试（每分钟最多" + MAX_REQUESTS_PER_MINUTE + "次）");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 返回429限流响应
     */
    private void writeRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        R<String> result = R.error(StatusCode.TOO_MANY_REQUESTS.getCode(), message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
