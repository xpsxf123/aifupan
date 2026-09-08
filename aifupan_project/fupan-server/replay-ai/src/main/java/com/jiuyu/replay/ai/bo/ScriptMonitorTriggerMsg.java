package com.jiuyu.replay.ai.bo;

import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 话术智能监控报告异步生成 MQ 消息体。
 *
 * <p>由 {@link com.jiuyu.replay.ai.bll.ScriptMonitorBll#triggerReport} 投递到本地消息表，
 * XXL-Job 扫描发送到 RocketMQ；由 {@link com.jiuyu.replay.ai.listener.ScriptMonitorMqListener}
 * 消费后调用 {@link com.jiuyu.replay.ai.bll.ScriptMonitorMqHandler#handle} 处理。</p>
 *
 * <p>幂等保证：消费侧根据 reportId 查 tb_script_monitor_report 当前 status，
 * 已成功(GENERATED=2)/不可用(NOT_APPLICABLE=4) 状态直接跳过；
 * GENERATING(1)/GENERATE_FAILED(3) 状态走 generate() 主流程。</p>
 *
 * @author beta
 * @date 2026-06-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptMonitorTriggerMsg implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 报告 ID（tb_script_monitor_report.id）
     */
    private Long reportId;

    /**
     * 触发前原始 status（用于 D-7 异步失败时恢复旧成功报告：
     * GENERATED 重试失败时回滚回 GENERATED，否则置 GENERATE_FAILED）
     */
    private Integer originalStatus;

    /**
     * Token 预扣凭据（含 withholdId/redisId/num，用于 settle/return）
     */
    private RedisWithholdVo withhold;
}
