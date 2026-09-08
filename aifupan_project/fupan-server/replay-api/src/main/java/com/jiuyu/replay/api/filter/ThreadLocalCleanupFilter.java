package com.jiuyu.replay.api.filter;

import com.jiuyu.replay.api.utils.ThreadLocalManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ThreadLocal资源清理过滤器
 * 专门用于清理各种ThreadLocal资源，防止内存泄漏
 *
 * @author RayChou
 * @date 2025/6/20 11:00
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // 确保在所有过滤器之前执行，这样在请求结束时能最后执行清理 但是在跨域请求处理之后
public class ThreadLocalCleanupFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ThreadLocalCleanupFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 执行后续过滤器链
            filterChain.doFilter(request, response);
        } finally {
            // 请求处理完成后使用ThreadLocalManager清理所有ThreadLocal资源
            cleanupAllThreadLocals();
        }
    }

    /**
     * 清理所有ThreadLocal资源
     * 使用ThreadLocalManager集中管理所有需要清理的ThreadLocal资源
     */
    private void cleanupAllThreadLocals() {
        try {
            // 使用ThreadLocalManager清理所有注册的ThreadLocal资源
            ThreadLocalManager.clearAll();
            
            if (log.isTraceEnabled()) {
                log.trace("所有ThreadLocal资源清理成功");
            }
        } catch (Exception e) {
            log.warn("清理ThreadLocal资源时发生异常: {}", e.getMessage());
        }
    }
} 