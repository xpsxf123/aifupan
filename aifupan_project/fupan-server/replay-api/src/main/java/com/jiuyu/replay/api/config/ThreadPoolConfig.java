package com.jiuyu.replay.api.config;

import com.jiuyu.replay.api.utils.ThreadLocalManager;
import com.jiuyu.replay.common.context.RequestContext;
import com.jiuyu.replay.common.utils.RRException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 提供支持ThreadLocal上下文传递的线程池
 *
 * @author RayChou
 * @date 2025/6/20 11:00
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 定义支持ThreadLocal上下文传递的线程池
     *
     * @return 线程池执行器
     */
    @Bean("threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(10);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量
        executor.setQueueCapacity(200);
        // 线程名前缀
        executor.setThreadNamePrefix("thread-pool-");
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        // 拒绝策略：调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 设置线程装饰器，用于传递ThreadLocal上下文
        executor.setTaskDecorator(new ContextCopyingDecorator());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean("generateHtmlExecutor")
    public Executor generateHtmlExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 线程核心数
        executor.setCorePoolSize(12);
        // 线程最大数
        executor.setMaxPoolSize(128);
        // 线程队列最大数
        executor.setQueueCapacity(16);
        // 线程前缀名
        executor.setThreadNamePrefix("generate-html-");
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 旧版异步线程池
     *
     * @return
     */
    @Deprecated
    @Bean
    public Executor asyncExecutor() {
        return new ThreadPoolTaskExecutor() {{
            setCorePoolSize(6);
            setMaxPoolSize(32);
            setQueueCapacity(18);
            new ThreadPoolExecutor.CallerRunsPolicy();
        }};
    }

    /**
     * ai调用外部异步线程池
     *
     * @return
     */
    @Bean
    public Executor asyncOutExecutor() {
        return new ThreadPoolTaskExecutor() {{
            setCorePoolSize(6);
            setMaxPoolSize(32);
            setKeepAliveSeconds(60);
            setQueueCapacity(0);
//            new ThreadPoolExecutor.CallerRunsPolicy();
            //这是静默丢弃策略
            setRejectedExecutionHandler(new CustomRejectedHandler());
        }};
    }

    /**
     * ai调用异步线程池
     *
     * @return
     */
    @Bean
    public Executor asyncInnerExecutor() {
        return new ThreadPoolTaskExecutor() {{
            setCorePoolSize(16);
            setMaxPoolSize(128);
            setKeepAliveSeconds(60);
            setQueueCapacity(16);
            //这是由提交任务的线程（调用者）直接执行任务，会阻塞调用者
            setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        }};
    }

    /**
     * 拒绝策略
     */
    static class CustomRejectedHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            throw new RRException("任务被线程池拒绝，当前状态：" + "线程池已满[" + executor.getPoolSize() + "/" + executor.getMaximumPoolSize() + "]，" + "队列已满[" + executor.getQueue().size() + "/" + executor.getQueue().remainingCapacity() + "]", 30001);
        }
    }

    /**
     * ThreadLocal上下文传递装饰器
     * 在任务执行前复制当前线程的ThreadLocal变量到工作线程
     */
    static class ContextCopyingDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            // 获取主线程的上下文
            String requestId = RequestContext.getRequestId();

            // 返回一个新的Runnable，在子线程中传递上下文
            return () -> {
                try {
                    // 在子线程中设置上下文
                    if (requestId != null) {
                        RequestContext.setRequestId(requestId);
                    }

                    // 执行原始任务
                    runnable.run();
                } finally {
                    // 清理子线程的ThreadLocal变量
                    ThreadLocalManager.clearAll();
                }
            };
        }
    }
} 