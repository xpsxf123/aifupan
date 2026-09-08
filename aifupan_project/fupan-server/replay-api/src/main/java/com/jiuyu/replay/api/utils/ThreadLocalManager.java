package com.jiuyu.replay.api.utils;

import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.power.utils.GlobalObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * ThreadLocal资源统一管理工具类
 * 用于集中管理和清理ThreadLocal资源，防止内存泄漏
 *
 * @author RayChou
 * @date 2025/6/20 11:00
 */
public class ThreadLocalManager {
    private static final Logger log = LoggerFactory.getLogger(ThreadLocalManager.class);

    // 使用线程安全的列表存储所有需要清理的ThreadLocal资源的清理器
    private static final List<Runnable> THREAD_LOCAL_CLEANERS = new CopyOnWriteArrayList<>();

    // 私有构造函数，防止实例化
    private ThreadLocalManager() {
    }

    /**
     * 静态初始化块，注册系统中所有已知的ThreadLocal清理器
     */
    static {

        // 注册RequestContext的ThreadLocal清理器
        registerCleaner("RequestContext", RequestContext::clear);

        // 可以继续注册其他ThreadLocal清理器，如GlobalObject等
        registerCleaner("GlobalObject", GlobalObject::removeLocalUser);
    }

    /**
     * 注册ThreadLocal清理器
     *
     * @param name    清理器名称，用于日志
     * @param cleaner 清理器函数
     */
    public static void registerCleaner(String name, Runnable cleaner) {
        THREAD_LOCAL_CLEANERS.add(() -> {
            try {
                cleaner.run();
                if (log.isTraceEnabled()) {
                    log.trace("ThreadLocal资源 [{}] 清理成功", name);
                }
            } catch (Exception e) {
                log.warn("ThreadLocal资源 [{}] 清理失败: {}", name, e.getMessage());
            }
        });
    }

    /**
     * 清理所有已注册的ThreadLocal资源
     */
    public static void clearAll() {
        for (Runnable cleaner : THREAD_LOCAL_CLEANERS) {
            cleaner.run();
        }
    }

    /**
     * 创建一个ThreadLocal包装器，自动注册到管理器
     *
     * @param <T>          ThreadLocal存储的对象类型
     * @param name         ThreadLocal的名称
     * @param initialValue 初始值提供器
     * @return 包装后的ThreadLocal对象
     */
    public static <T> ThreadLocal<T> createThreadLocal(String name, Supplier<T> initialValue) {
        ThreadLocal<T> threadLocal = ThreadLocal.withInitial(initialValue);
        registerCleaner(name, threadLocal::remove);
        return threadLocal;
    }

    /**
     * 在try-finally块中执行代码，确保ThreadLocal资源被清理
     *
     * @param runnable 要执行的代码
     */
    public static void runWithCleanup(Runnable runnable) {
        try {
            runnable.run();
        } finally {
            clearAll();
        }
    }
} 