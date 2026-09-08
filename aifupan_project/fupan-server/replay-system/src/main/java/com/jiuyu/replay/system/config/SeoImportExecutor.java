package com.jiuyu.replay.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SEO 文章批量导入专用线程池。
 *
 * <p><b>为什么不复用 replay-common 的 ExecutorUtil</b>：
 * 一是改 replay-common 会触发 Standard HIGH 流程；二是那两个池的拒绝策略都不适合导入——
 * CallerRunsPolicy 会把一次可能长达数十秒（含图片抓取）的导入压回 Tomcat 线程，
 * 接口不再是「立即返回 taskId」；DiscardPolicy 则让任务无声消失，前端永远轮询到「处理中」。
 *
 * <p>本池用有界队列 + {@link RejectedExecutionException}：提交侧捕获后把任务直接落为
 * 「失败」并写明原因，运营立刻看到「导入队列已满」而不是转圈。
 *
 * <h3>参数依据</h3>
 * 导入是 I/O 密集（下载图片、写库）但并发需求极低——后台运营手工触发，
 * 同时进行的任务不会超过个位数。常态是 2 个线程在跑、后来的排进队列；
 * 队列（16）满了才扩到 4 个线程，再满就拒绝。这个顺序是 ThreadPoolExecutor 的固有行为，
 * 也正是这里想要的：宁可让任务排队，也不要为一次运营操作起一堆线程去抢带宽。
 *
 * @author claude
 * @date 2026-08-13
 */
public final class SeoImportExecutor {

    private static final Logger log = LoggerFactory.getLogger(SeoImportExecutor.class);

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {

        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "seo-import-" + counter.getAndIncrement());
            // 守护线程：导入任务的结果已随进度落库，进程关闭时没必要为它阻塞退出
            thread.setDaemon(true);
            return thread;
        }
    };

    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(
            2,
            4,
            120L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(16),
            THREAD_FACTORY,
            (runnable, executor) -> {
                log.warn("[SEO导入] 线程池已满(core=2,max=4,queue=16,active={},queued={})",
                        executor.getActiveCount(), executor.getQueue().size());
                throw new RejectedExecutionException("导入队列已满");
            });

    private SeoImportExecutor() {
    }

    /**
     * 提交一个导入任务。
     *
     * @param task 任务体
     * @throws RejectedExecutionException 队列已满，调用方须把任务标记为失败
     */
    public static void submit(Runnable task) {
        POOL.execute(task);
    }
}
