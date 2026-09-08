package com.jiuyu.replay.ai.task;

import cn.hutool.core.collection.CollUtil;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.common.constant.LockKeyPrefix;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 话术智能监控报告"僵尸任务"恢复 XXL-Job（P2 修补）。
 *
 * <p>触发场景：
 * <ul>
 *   <li>Tomcat 重启时 RocketMQ consumer thread 内未消费完的消息丢失（at-least-once 不保证）</li>
 *   <li>消息投到死信队列（消费失败 N 次）业务侧无感</li>
 *   <li>consumer worker 假死（JVM 卡顿 / 网络分区）</li>
 * </ul>
 * 这些情况下 tb_script_monitor_report 的 status 会卡在 1(GENERATING) 永不变化，
 * 用户看到的报告永远"生成中"。本 job 定时扫描超时记录，主动改 status=3(GENERATE_FAILED)
 * 让用户重新触发即可恢复。</p>
 *
 * <p>XXL-Job 控制台配置 cron 建议 {@code 0 *\/5 * * * ?}（每 5 分钟跑一次），
 * 阈值 30 分钟（业务上 generate() 最坏 10min 完成，30min 仍 GENERATING = 假死）。</p>
 *
 * @author beta
 * @date 2026-06-01
 */
@Slf4j
@Component
@AllArgsConstructor
public class ScriptMonitorReportZombieHandler {

    /**
     * 僵尸阈值：超过此时长仍 status=1 视为假死（30 分钟）。
     */
    private static final long ZOMBIE_THRESHOLD_MINUTES = 30L;

    /**
     * 单次扫描最大处理条数（防一次 update 过多）。
     */
    private static final int BATCH_LIMIT = 200;

    /**
     * 分布式锁超时（避免多实例并发扫描）。
     */
    private static final long LOCK_WAIT_SECONDS = 3L;
    private static final long LOCK_HOLD_SECONDS = 60L;

    private final ScriptMonitorReportService reportService;
    private final RedissonClient redissonClient;

    /**
     * 扫描超时的 GENERATING 报告并改 GENERATE_FAILED。
     *
     * <p>XXL-Job handler name：{@code scriptMonitorReportZombieRecovery}</p>
     */
    @XxlJob("scriptMonitorReportZombieRecovery")
    public void recoverZombieReports() {
        RLock lock = redissonClient.getLock(LockKeyPrefix.MQ.getLockKey("scriptMonitorZombieRecovery"));
        try {
            if (!lock.tryLock(LOCK_WAIT_SECONDS, LOCK_HOLD_SECONDS, TimeUnit.SECONDS)) {
                log.info("[script-monitor-zombie] 获取锁失败，跳过本次执行");
                return;
            }

            Date threshold = new Date(System.currentTimeMillis() - ZOMBIE_THRESHOLD_MINUTES * 60 * 1000L);
            List<ScriptMonitorReportEntity> zombies = reportService.lambdaQuery()
                    .eq(ScriptMonitorReportEntity::getStatus, ScriptMonitorStatusEnum.GENERATING.getCode())
                    .eq(ScriptMonitorReportEntity::getIsDeleted, 0)
                    .lt(ScriptMonitorReportEntity::getUpdateDate, threshold)
                    .last("LIMIT " + BATCH_LIMIT)
                    .list();

            if (CollUtil.isEmpty(zombies)) {
                return;
            }

            int recovered = 0;
            Date now = new Date();
            for (ScriptMonitorReportEntity zombie : zombies) {
                try {
                    zombie.setStatus(ScriptMonitorStatusEnum.GENERATE_FAILED.getCode());
                    zombie.setUnavailableReason("系统未响应（超时 " + ZOMBIE_THRESHOLD_MINUTES + " 分钟），请重新触发");
                    zombie.setUpdateDate(now);
                    if (reportService.updateById(zombie)) {
                        recovered++;
                    }
                } catch (Exception ex) {
                    log.error("[script-monitor-zombie] 恢复单条失败 reportId={}", zombie.getId(), ex);
                }
            }
            log.info("[script-monitor-zombie] 本次扫描共 {} 条僵尸，恢复 {} 条", zombies.size(), recovered);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[script-monitor-zombie] 执行中断", e);
        } catch (Exception e) {
            log.error("[script-monitor-zombie] 执行异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
