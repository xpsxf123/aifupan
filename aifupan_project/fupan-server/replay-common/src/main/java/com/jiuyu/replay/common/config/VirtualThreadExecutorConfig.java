package com.jiuyu.replay.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * JDK 21 虚拟线程 executor 配置（供业务 fan-out 并发 AI 调用使用）。
 *
 * <p>本 executor 不限制并发数（每任务一虚拟线程），适合 IO 密集 + 阻塞等待场景；
 * 对火山 Ark / DeepSeek 等外部 AI 服务的配额限流由调用方按需补全（本期不实施，
 * 由 ADR-4 显式接受风险，监控 429 频率后再决定是否补全局 Semaphore）。</p>
 *
 * <p>使用方：{@code ScriptMonitorGenerateBll} / {@code ScriptMonitorPatrolGenerateBll}
 * / {@code ScriptMonitorFidelityGenerateBll}，通过 final 字段 + Lombok 构造器注入，
 * 字段名约定为 {@code scriptMonitorVtExecutor}（与 Bean 名一致，Spring 按 byName fallback 注入）。</p>
 *
 * @author claude
 * @since 2026-06-23 阶段三 fan-out 改造
 */
@Configuration
public class VirtualThreadExecutorConfig {

    /**
     * 用于 ScriptMonitor 三个 Bll 的 fan-out 并发 AI 调用。
     *
     * <p>每提交一个任务即创建一个虚拟线程（JDK 21 不复用、用完即弃）；
     * 虚拟线程在 AI Feign 阻塞 IO 期间会卸载载体线程，让其他虚拟线程接力跑。</p>
     *
     * <p>Bean 名 {@code scriptMonitorVtExecutor} 与字段名一致，使用方无需 @Qualifier。</p>
     *
     * @return 虚拟线程 executor
     */
    @Bean("scriptMonitorVtExecutor")
    public Executor scriptMonitorVtExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
