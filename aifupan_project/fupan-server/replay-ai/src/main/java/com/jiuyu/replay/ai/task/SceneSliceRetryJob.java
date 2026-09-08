package com.jiuyu.replay.ai.task;

import cn.hutool.core.collection.CollUtil;
import com.jiuyu.replay.ai.entity.SceneSliceEntity;
import com.jiuyu.replay.ai.rse.SceneSliceRse;
import com.jiuyu.replay.ai.bll.SceneSliceBll;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 场景切片"僵尸任务"恢复 XXL-Job。
 *
 * <p>触发场景：服务重启/异常导致 PROCESSING 状态记录卡住。
 * 定时扫描超时记录，重新触发 AI 分析；再次失败则置 FAILED。</p>
 *
 * <p>XXL-Job 控制台配置 cron 建议 {@code 0 *\/5 * * * ?}（每 5 分钟执行一次）。</p>
 *
 * @author lj
 * @date 2026-07-06
 */
@Slf4j
@Component
@AllArgsConstructor
public class SceneSliceRetryJob {

    private static final long LOCK_WAIT_SECONDS = 3L;
    private static final long LOCK_HOLD_SECONDS = 60L;
    private static final int DEFAULT_TIMEOUT_MINUTES = 5;

    private final SceneSliceRse sceneSliceRse;
    private final SceneSliceBll sceneSliceBll;
    private final RedissonClient redissonClient;

    /**
     * 扫描超时的 PROCESSING 记录并重新触发 AI 分析。
     *
     * <p>XXL-Job handler name：{@code sceneSliceRetry}</p>
     */
    @XxlJob("sceneSliceRetry")
    public void retryStuckSlices() {
        RLock lock = redissonClient.getLock(LockKeyPrefix.MQ.getLockKey("sceneSliceRetry"));
        try {
            if (!lock.tryLock(LOCK_WAIT_SECONDS, LOCK_HOLD_SECONDS, TimeUnit.SECONDS)) {
                log.info("[scene-slice-retry] 获取锁失败，跳过本次执行");
                return;
            }

            List<SceneSliceEntity> stuckList = sceneSliceRse.listStuckProcessing(DEFAULT_TIMEOUT_MINUTES);
            if (CollUtil.isEmpty(stuckList)) {
                return;
            }

            int retried = 0;
            for (SceneSliceEntity entity : stuckList) {
                try {
                    log.info("[scene-slice-retry] 重新触发AI分析，sliceId={}", entity.getId());
                    sceneSliceBll.triggerAiAnalysis(entity.getId(), entity.getOssKey());
                    retried++;
                } catch (Exception ex) {
                    log.error("[scene-slice-retry] 重试失败 sliceId={}", entity.getId(), ex);
                }
            }
            log.info("[scene-slice-retry] 本次扫描共 {} 条超时记录，重试触发 {} 条", stuckList.size(), retried);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[scene-slice-retry] 执行中断", e);
        } catch (Exception e) {
            log.error("[scene-slice-retry] 执行异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
