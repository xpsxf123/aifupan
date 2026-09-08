package com.jiuyu.replay.api.task;

import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.order.bll.PowerRollupBll;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 算力消耗汇总定时刷新任务
 * cron 由 XXL-Job Admin 配置为 T+1 凌晨
 * ⚠️ 阻塞策略必须配「单机串行 / 丢弃后续调度」——本任务为增量累加，并发执行会重复累加
 *
 * @author lead-engineer
 * @date 2026-07-24
 */
@Component
@Slf4j
public class PowerRollupScheduledTasks {

    private final PowerRollupBll powerRollupBll;

    /**
     * 构造器注入
     *
     * @param powerRollupBll 算力汇总刷新 bll
     */
    public PowerRollupScheduledTasks(PowerRollupBll powerRollupBll) {
        this.powerRollupBll = powerRollupBll;
    }

    /**
     * 定时增量刷新用户/租户算力消耗汇总表
     * 水位线未初始化等业务前置条件不满足时，必须让 XXL-Job Admin 呈现为「失败」，
     * 否则运维收不到告警，汇总长期不产出也无人发现
     *
     * @return 执行结果
     */
    @XxlJob("timingUpdatePowerConsume")
    public R<String> timingUpdatePowerConsume() {
        log.info("[定时更新算力消耗汇总] 开始执行");
        long start = System.currentTimeMillis();
        try {
            powerRollupBll.refreshPowerRollup();
        } catch (BusinessException e) {
            // 前置条件不满足（如存量未初始化 → 水位线行缺失）：标记任务失败，不吞异常语义
            log.error("[定时更新算力消耗汇总] 执行失败：{}", e.getMessage(), e);
            XxlJobHelper.handleFail("[定时更新算力消耗汇总] 执行失败：" + e.getMessage());
            return R.error(e.getMessage());
        }
        log.info("[定时更新算力消耗汇总] 执行完成 用时{}ms", System.currentTimeMillis() - start);
        return R.ok("");
    }
}
