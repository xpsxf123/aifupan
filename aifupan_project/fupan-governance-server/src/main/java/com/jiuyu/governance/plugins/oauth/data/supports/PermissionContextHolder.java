package com.jiuyu.governance.plugins.oauth.data.supports;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 数据权限上下文管理器
 * 统一管理线程级别和请求级别的权限忽略状态及缓存，确保安全的 try-finally 清理范式。
 *
 * @author HeHui
 * @date 2026-03-18
 */
public class PermissionContextHolder {

    private static final ThreadLocal<Boolean> IGNORE_THREAD_LOCAL = new InheritableThreadLocal<>();
    private static final String REQUEST_IGNORE_ATTR = PermissionContextHolder.class.getName() + ".IGNORE";
    private static final String REQUEST_CACHE_ATTR = PermissionContextHolder.class.getName() + ".CACHE";

    /**
     * 判断当前是否允许忽略权限检查
     */
    public static boolean isIgnore() {
        if (Boolean.TRUE.equals(IGNORE_THREAD_LOCAL.get())) {
            return true;
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            Object isIgnore = requestAttributes.getAttribute(REQUEST_IGNORE_ATTR, RequestAttributes.SCOPE_REQUEST);
            if (isIgnore instanceof Boolean) {
                return (Boolean) isIgnore;
            }
        }
        return false;
    }

    /**
     * 设置当前请求范围内忽略权限检查
     */
    public static void setRequestIgnore() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(REQUEST_IGNORE_ATTR, true, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * 清除请求范围内的权限忽略标志
     */
    public static void clearRequestIgnore() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.removeAttribute(REQUEST_IGNORE_ATTR, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * 在忽略权限检查的环境下执行给定的 Supplier 函数
     * 内部使用标准的 try-finally 清理范式，避免内存泄漏或权限状态污染
     */
    public static <T> T runWithIgnore(Supplier<T> supplier) {
        boolean original = Boolean.TRUE.equals(IGNORE_THREAD_LOCAL.get());
        if (!original) {
            IGNORE_THREAD_LOCAL.set(true);
        }
        try {
            return supplier.get();
        } finally {
            if (!original) {
                IGNORE_THREAD_LOCAL.remove();
            }
        }
    }

    /**
     * 在忽略权限检查的环境下执行给定的 Runnable 任务
     */
    public static void runWithIgnore(Runnable runnable) {
        runWithIgnore(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 获取请求级缓存
     */
    @SuppressWarnings("unchecked")
    public static <T> T getRequestCache(String dataType) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            return (T) requestAttributes.getAttribute(REQUEST_CACHE_ATTR + "." +dataType, RequestAttributes.SCOPE_REQUEST);
        }
        return null;
    }

    /**
     * 设置请求级缓存
     */
    public static void setRequestCache(String dataType, Object cacheObj) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(REQUEST_CACHE_ATTR + "." + dataType, cacheObj, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * 清除请求级缓存
     */
    public static void clearRequestCache() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.removeAttribute(REQUEST_CACHE_ATTR, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * 清除当前线程和请求的所有权限上下文状态
     */
    public static void clearAll() {
        IGNORE_THREAD_LOCAL.remove();
        clearRequestIgnore();
        clearRequestCache();
    }
}
