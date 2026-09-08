package com.jiuyu.replay.words.bll;

import cn.hutool.core.util.ObjectUtil;
import com.jiuyu.replay.common.constant.WordsEnum;
import com.jiuyu.replay.common.producer.SystemKvProducer;
import com.jiuyu.replay.common.utils.ExecutorUtil;
import com.jiuyu.replay.generic.feign.order.AiTokenUseRecordFeign;
import com.jiuyu.replay.generic.feign.ai.AiChatFeign;
import com.jiuyu.replay.generic.feign.third.AiModelFeign;
import com.jiuyu.replay.words.producer.AnchorVideoDetailProducer;
import com.jiuyu.replay.words.producer.UploadFileDetailProducer;
import com.jiuyu.replay.words.producer.VideoContentProducer;
import com.jiuyu.replay.words.vo.video.SegmentResult;
import com.jiuyu.replay.words.vo.video.ToGeneratedVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自然/优化原文批次生成引擎。
 *
 * <h3>职责</h3>
 * 接收 Logic 层组装好的分段数据（{@link ToGeneratedVo}），在线程池中异步执行整个批次。
 * 一个批次 = 一个视频/文件的自然原文 或 优化原文任务，包含多个分段（段落）。
 *
 * <h3>线程模型</h3>
 * 使用 {@link ExecutorUtil#videoContentGenPool}（core=4, max=60, SynchronousQueue, CallerRunsPolicy）。
 * 每个批次占用一个线程，批次内所有分段通过 {@link ThreadPoolExecutor#invokeAll} 全部并行执行。
 * 并发粒度：线程池大小控制同时跑几个批次，段级别不做额外限制。
 *
 * <h3>结果汇总</h3>
 * 所有分段完成后汇总：
 * <ul>
 *   <li>全部/部分成功 → 更新 DB 状态为 SUCCESS（status=2）</li>
 *   <li>全部失败 → 更新 DB 状态为 FAILED（status=3）并重置 set_job=0 允许定时器重试</li>
 * </ul>
 *
 * @author lujie
 * @date 2025/6/24
 */
@Component
@Slf4j
public class VideoContentGenerator {

    @Resource
    private VideoContentProducer videoContentProducer;
    @Resource
    private AiContentCorrectService aiContentCorrectService;
    @Resource
    private SystemKvProducer systemKvProducer;
    @Resource
    private AiModelFeign aiModelFeign;
    @Resource
    private AiChatFeign aiChatFeign;
    @Resource
    private AiTokenUseRecordFeign aiTokenUseRecordFeign;
    @Resource
    private AnchorVideoDetailProducer anchorVideoDetailProducer;
    @Resource
    private UploadFileDetailProducer uploadFileDetailProducer;

    /**
     * 专用线程池：core=4, max=60, SynchronousQueue（无缓冲）, CallerRunsPolicy（溢出时由调用线程执行并打印日志）
     */
    private final ThreadPoolExecutor executor = ExecutorUtil.videoContentGenPool;

    /**
     * 异步提交批次任务到线程池。
     * 调用后立即返回，不等待 AI 调用完成。最终状态由线程池内的任务自行更新 DB。
     *
     * @param batch 批次数据，包含 sourceId、sourceType、type 和分段列表
     */
    public void executeBatch(ToGeneratedVo batch) {
        executor.submit(() -> doExecuteBatch(batch));
    }

    /**
     * 批次执行的核心逻辑（在线程池线程中执行）。
     *
     * <h3>执行流程</h3>
     * <ol>
     *   <li>校验分段列表是否为空</li>
     *   <li>为每个分段创建 {@link SegmentTask}（Callable，含重试逻辑）</li>
     *   <li>通过 {@code executor.invokeAll(tasks, 10, MINUTES)} 并行执行所有分段</li>
     *   <li>汇总结果：统计成功/失败数量</li>
     *   <li>根据汇总结果更新 DB 最终状态</li>
     * </ol>
     *
     * <h3>超时处理</h3>
     * 整批超时时间为 10 分钟。超时后未完成的分段对应的 Future 会抛出 {@link CancellationException}，
     * 已完成的依然正常汇总。
     *
     * @param batch 批次数据
     */
    private void doExecuteBatch(ToGeneratedVo batch) {
        log.info("[批次执行] 开始, sourceId={}, sourceType={}, type={}, segments={}",
                batch.getSourceId(), batch.getSourceType(), batch.getType(),
                batch.getVideoContentList() != null ? batch.getVideoContentList().size() : 0);

        // 空分段直接标记失败
        if (ObjectUtil.isEmpty(batch.getVideoContentList())) {
            log.warn("[批次执行] 无分段数据, sourceId={}", batch.getSourceId());
            updateStatus(batch, WordsEnum.contentStatus.FAILED.getCode(), true);
            return;
        }

        // 为每个分段构造 SegmentTask（Callable，内部含 3 次重试 + 格式纠正）
        List<SegmentTask> tasks = batch.getVideoContentList().stream()
                .map(seg -> new SegmentTask(seg, aiContentCorrectService, videoContentProducer,
                        systemKvProducer, aiModelFeign, aiChatFeign, aiTokenUseRecordFeign))
                .toList();

        List<SegmentResult> results = new ArrayList<>();
        try {
            // invokeAll 并行执行所有分段，整批超时 10 分钟
            List<Future<SegmentResult>> futures = executor.invokeAll(tasks, 10, TimeUnit.MINUTES);
            for (Future<SegmentResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (CancellationException e) {
                    // 超时未完成的分段会被取消
                    log.warn("[批次执行] 分段超时被取消");
                } catch (ExecutionException e) {
                    // 分段内部抛出的异常（通常已被 SegmentTask 内部捕获，此处为兜底）
                    log.warn("[批次执行] 分段执行异常", e.getCause());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[批次执行] 整批被中断, sourceId={}", batch.getSourceId());
            // 中断时 results 为空，必须在此处标记失败，否则下方汇总逻辑会误判为 SUCCESS
            updateStatus(batch, WordsEnum.contentStatus.FAILED.getCode(), true);
            return;
        }

        long successCount = results.stream().filter(SegmentResult::isSuccess).count();
        long failCount = results.size() - successCount;

        log.info("[批次执行] 完成, sourceId={}, total={}, success={}, fail={}",
                batch.getSourceId(), results.size(), successCount, failCount);

        // 全部失败 → 标记 FAILED + 重置 set_job=0 允许重试
        // 只要有一个成功就不再重置 set_job（部分成功也视为完成）
        if (failCount == results.size() && results.size() > 0) {
            updateStatus(batch, WordsEnum.contentStatus.FAILED.getCode(), true);
        } else {
            updateStatus(batch, WordsEnum.contentStatus.SUCCESS.getCode(), false);
        }
    }

    /**
     * 更新 MySQL 中视频/文件的内容生成状态。
     * 根据 sourceType 路由到 {@link AnchorVideoDetailProducer} 或 {@link UploadFileDetailProducer}。
     *
     * @param batch    批次数据（含 sourceId、sourceType、type）
     * @param status   目标状态：2=SUCCESS, 3=FAILED
     * @param resetJob 是否同时重置 set_job=0（全部失败时允许定时器重新拾取）
     */
    private void updateStatus(ToGeneratedVo batch, int status, boolean resetJob) {
        try {
            if (batch.getSourceType() == WordsEnum.sourceType.VIDEO.getCode()) {
                anchorVideoDetailProducer.updateContentStatusBySourceId(
                        batch.getSourceId(), batch.getType(), status, resetJob);
            } else if (batch.getSourceType() == WordsEnum.sourceType.FILE.getCode()) {
                uploadFileDetailProducer.updateContentStatusBySourceId(
                        batch.getSourceId(), batch.getType(), status, resetJob);
            }
            log.info("[批次执行] 状态更新完成, sourceId={}, status={}, resetJob={}",
                    batch.getSourceId(), status, resetJob);
        } catch (Exception e) {
            log.error("[批次执行] 状态更新失败, sourceId={}", batch.getSourceId(), e);
        }
    }
}
