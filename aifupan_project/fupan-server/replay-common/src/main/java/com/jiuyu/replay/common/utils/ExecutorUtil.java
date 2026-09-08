package com.jiuyu.replay.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/4/9 下午4:56
 */
@Slf4j
public class ExecutorUtil {

    /**
     * AI SSE 对话专用线程池。
     *
     * <h3>配置说明</h3>
     * <ul>
     *   <li>core=16, max=128：AI SSE 任务为长时 I/O 阻塞操作，日常 16 常驻线程足够，高峰自动扩容至 128</li>
     *   <li>SynchronousQueue（无缓冲队列）：不排队，任务到达时如果有空闲线程则立即执行，
     *       没有则创建新线程（不超过 max），超过 max 则触发拒绝策略</li>
     *   <li>CallerRunsPolicy + 日志：线程池满时由调用线程（Tomcat HTTP 线程）同步执行，
     *       牺牲 HTTP 线程响应换取不丢失请求。连续打印 3 次告警日志方便监控发现</li>
     * </ul>
     *
     * <h3>调用方</h3>
     * <ul>
     *   <li>{@code AiRelatedLogicImpl} — AI 问答 SSE 流式调用</li>
     *   <li>{@code DataScreenshotLogicImpl} — 数据截图分析</li>
     * </ul>
     */
    public static final ThreadPoolExecutor customPool = new ThreadPoolExecutor(
            16,
            128,
            120,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            (r, executor) -> {
                for (int i = 0; i < 3; i++) {
                    log.warn("[customPool] 线程池已满(core=16,max=128,active={},task={}), 由调用线程执行",
                            executor.getActiveCount(), executor.getTaskCount());
                }
                new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(r, executor);
            }
    );

    /**
     * 自然/优化原文生成专用线程池。
     *
     * <h3>配置说明</h3>
     * <ul>
     *   <li>core=4, max=60：原文生成是典型的 I/O 密集型任务（等待 AI 返回），
     *       可超 CPU 核数配置，瓶颈在内存而非 CPU</li>
     *   <li>SynchronousQueue（无缓冲队列）：同 customPool，不排队直接创建线程</li>
     *   <li>CallerRunsPolicy + 日志：溢出时由 XXL-Job 调度线程同步执行，
     *       连续打印 3 次告警日志方便监控发现</li>
     *   <li>keepAlive=120s：空闲线程 2 分钟后回收，核心线程不回收</li>
     * </ul>
     *
     * <h3>并发粒度</h3>
     * 线程数控制的是同时执行的<b>批次数</b>（一个批次 = 一个视频/文件的自然或优化原文）。
     * 批次内的分段不做额外限制，全部并行执行（与 C# 客户端 Task.WhenAll 行为一致）。
     *
     * <h3>调用方</h3>
     * <ul>
     *   <li>{@code VideoContentGenerator} — 自然/优化原文批次生成</li>
     * </ul>
     */
    public static final ThreadPoolExecutor videoContentGenPool = new ThreadPoolExecutor(
            4,
            60,
            120L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            (r, executor) -> {
                for (int i = 0; i < 3; i++) {
                    log.warn("[VideoContentGen] 线程池已满(core=4,max=60,active={},task={}), 由调用线程执行",
                            executor.getActiveCount(), executor.getTaskCount());
                }
                new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(r, executor);
            }
    );


    private static volatile ThreadPoolExecutor anchorKeywordTaskExecutor = null;

    /**
     * 获取AI生成主播关键词的线程池
     * @return
     */
    public static ThreadPoolExecutor getAnchorKeywordTaskExecutor() {
        if (anchorKeywordTaskExecutor == null) {
            synchronized (ExecutorUtil.class) {
                if (anchorKeywordTaskExecutor == null) {
                    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
                    // 核心线程数
                    executor.setCorePoolSize(4);
                    // 最大线程数
                    executor.setMaxPoolSize(4);
                    // 队列容量
                    executor.setQueueCapacity(64);
                    // 线程名前缀
                    executor.setThreadNamePrefix("anchor-keyword-");
                    // 线程空闲时间
                    executor.setKeepAliveSeconds(120);
                    // 拒绝策略：直接丢弃
                    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
                    // 等待所有任务结束后再关闭线程池
                    executor.setWaitForTasksToCompleteOnShutdown(true);
                    executor.initialize();
                    anchorKeywordTaskExecutor = executor.getThreadPoolExecutor();
                }
            }
        }
        return anchorKeywordTaskExecutor;
    }

    private static volatile ThreadPoolExecutor importantBarrageTaskExecutor = null;

    public static ThreadPoolExecutor getImportantBarrageTaskExecutor() {
        if (importantBarrageTaskExecutor == null) {
            synchronized (ExecutorUtil.class) {
                if (importantBarrageTaskExecutor == null) {
                    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
                    // 核心线程数
                    executor.setCorePoolSize(16);
                    // 最大线程数
                    executor.setMaxPoolSize(32);
                    // 队列容量
                    executor.setQueueCapacity(64);
                    // 线程名前缀
                    executor.setThreadNamePrefix("important-barrage-");
                    // 线程空闲时间
                    executor.setKeepAliveSeconds(120);
                    // 拒绝策略：调用者线程执行
                    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                    // 等待所有任务结束后再关闭线程池
                    executor.setWaitForTasksToCompleteOnShutdown(true);
                    executor.initialize();
                    importantBarrageTaskExecutor = executor.getThreadPoolExecutor();
                }
            }

        }
        return importantBarrageTaskExecutor;
    }

}
