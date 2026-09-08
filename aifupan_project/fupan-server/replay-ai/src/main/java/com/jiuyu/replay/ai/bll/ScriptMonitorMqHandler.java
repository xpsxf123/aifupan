package com.jiuyu.replay.ai.bll;

import com.alibaba.fastjson2.JSON;
import com.jiuyu.replay.ai.bo.ScriptMonitorTriggerMsg;
import com.jiuyu.replay.ai.config.ScriptMonitorMqProperties;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 话术智能监控 MQ 消息业务处理器。
 *
 * <p>由 {@link com.jiuyu.replay.ai.listener.ScriptMonitorMqListener} 委托调用，
 * 按 tag 路由到对应的 monitor 生成流程：quality-inspection（B4，monitorType=0）、
 * fidelity-monitor（Slice B，monitorType=1）、interaction-patrol（T31-T36，monitorType=2）。</p>
 *
 * <p>消费返回语义：
 * <ul>
 *   <li>{@code SUCCESS} = 已处理（包含 generate 成功 / 业务异常已 catch 补偿 / 重复消息跳过）→ broker 不重试</li>
 *   <li>{@code FAILURE} = 系统级失败（NPE / 反序列化失败 / 数据库连接错误）→ broker 重试</li>
 * </ul>
 * </p>
 *
 * @author beta
 * @date 2026-06-01
 */
@Slf4j
@Component
public class ScriptMonitorMqHandler {

    private final ScriptMonitorReportService reportService;
    private final ScriptMonitorGenerateBll generateBll;
    private final ScriptMonitorFidelityGenerateBll fidelityGenerateBll;
    private final ScriptMonitorPatrolGenerateBll patrolGenerateBll;
    private final ScriptMonitorMqProperties mqProperties;

    /**
     * 构造器注入（禁 @Autowired / @Resource）。
     *
     * @param reportService         报告 Service
     * @param generateBll           质检生成 Bll
     * @param fidelityGenerateBll   还原度生成 Bll（Slice B）
     * @param patrolGenerateBll     互动巡检生成 Bll
     * @param mqProperties          MQ 配置
     */
    public ScriptMonitorMqHandler(ScriptMonitorReportService reportService,
                                  ScriptMonitorGenerateBll generateBll,
                                  ScriptMonitorFidelityGenerateBll fidelityGenerateBll,
                                  ScriptMonitorPatrolGenerateBll patrolGenerateBll,
                                  ScriptMonitorMqProperties mqProperties) {
        this.reportService = reportService;
        this.generateBll = generateBll;
        this.fidelityGenerateBll = fidelityGenerateBll;
        this.patrolGenerateBll = patrolGenerateBll;
        this.mqProperties = mqProperties;
    }

    /**
     * 按 tag 路由处理 MQ 消息。
     *
     * @param tag        RocketMQ 消息 tag
     * @param body       JSON 序列化的 {@link ScriptMonitorTriggerMsg}
     * @param messageKey 消息 key（用于日志追踪）
     * @return 消费结果（SUCCESS 不重试 / FAILURE 重试）
     */
    public ConsumeResult handle(String tag, String body, String messageKey) {
        ScriptMonitorTriggerMsg msg;
        try {
            msg = JSON.parseObject(body, ScriptMonitorTriggerMsg.class);
        } catch (Exception e) {
            log.error("[script-monitor-handler] 消息体解析失败 messageKey={} body={}", messageKey, body, e);
            return ConsumeResult.SUCCESS;
        }
        if (msg == null || msg.getReportId() == null) {
            log.error("[script-monitor-handler] 消息体不完整 messageKey={} body={}", messageKey, body);
            return ConsumeResult.SUCCESS;
        }

        // 幂等校验：按 reportId 查当前 status
        ScriptMonitorReportEntity report = reportService.getById(msg.getReportId());
        if (report == null) {
            log.warn("[script-monitor-handler] 报告不存在 reportId={} messageKey={}",
                    msg.getReportId(), messageKey);
            return ConsumeResult.SUCCESS;
        }
        Integer status = report.getStatus();
        if (Objects.equals(status, ScriptMonitorStatusEnum.GENERATED.getCode())) {
            log.info("[script-monitor-handler] 报告已成功，跳过 reportId={} messageKey={}",
                    msg.getReportId(), messageKey);
            return ConsumeResult.SUCCESS;
        }
        if (Objects.equals(status, ScriptMonitorStatusEnum.NOT_APPLICABLE.getCode())) {
            log.info("[script-monitor-handler] 报告已置 NOT_APPLICABLE，跳过 reportId={} messageKey={}",
                    msg.getReportId(), messageKey);
            return ConsumeResult.SUCCESS;
        }
        // status=1 GENERATING / status=3 GENERATE_FAILED → 走 generate

        // tag 路由（B4 当前仅 quality）
        ScriptMonitorMqProperties.BusinessTags tags = mqProperties.getBusinessTags();
        if (tags.getTagQualityInspection() != null && tags.getTagQualityInspection().equals(tag)) {
            return doGenerate(report, msg, messageKey);
        }
        if (tags.getTagFidelityMonitor() != null && tags.getTagFidelityMonitor().equals(tag)) {
            return doFidelityGenerate(report, msg, messageKey);
        }
        if (tags.getTagInteractionPatrol() != null && tags.getTagInteractionPatrol().equals(tag)) {
            return doPatrolGenerate(report, msg, messageKey);
        }
        log.warn("[script-monitor-handler] 未知 tag={} reportId={} messageKey={}",
                tag, msg.getReportId(), messageKey);
        return ConsumeResult.SUCCESS;
    }

    /**
     * 执行质检报告生成。
     *
     * <p>{@code generate()} 内部已 try-catch 全部业务异常 + Token 返还 + 状态补偿，
     * 正常路径下不会抛 BusinessException 出来；这里再 catch 一层兜底防御性。</p>
     *
     * @param report     报告实体
     * @param msg        触发消息
     * @param messageKey 消息 key
     * @return 消费结果
     */
    private ConsumeResult doGenerate(ScriptMonitorReportEntity report, ScriptMonitorTriggerMsg msg, String messageKey) {
        try {
            generateBll.generate(report, msg.getOriginalStatus(), msg.getWithhold());
            return ConsumeResult.SUCCESS;
        } catch (BusinessException e) {
            log.warn("[script-monitor-handler] 业务异常 reportId={} messageKey={} code={} msg={}",
                    report.getId(), messageKey, e.getCode(), e.getMessage());
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            log.error("[script-monitor-handler] 系统异常 reportId={} messageKey={}",
                    report.getId(), messageKey, e);
            return ConsumeResult.FAILURE;
        }
    }

    /**
     * 执行话术还原度报告生成（Slice B）。
     *
     * <p>{@code generate()} 内部已 try-catch 全部业务异常 + Token 返还 + 状态补偿，
     * 正常路径下不会抛 BusinessException 出来；这里再 catch 一层兜底防御性。</p>
     *
     * @param report     报告实体
     * @param msg        触发消息
     * @param messageKey 消息 key
     * @return 消费结果
     */
    private ConsumeResult doFidelityGenerate(ScriptMonitorReportEntity report, ScriptMonitorTriggerMsg msg, String messageKey) {
        try {
            fidelityGenerateBll.generate(report, msg.getOriginalStatus(), msg.getWithhold());
            return ConsumeResult.SUCCESS;
        } catch (BusinessException e) {
            log.warn("[script-monitor-handler] 还原度业务异常 reportId={} messageKey={} code={} msg={}",
                    report.getId(), messageKey, e.getCode(), e.getMessage());
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            log.error("[script-monitor-handler] 还原度系统异常 reportId={} messageKey={}",
                    report.getId(), messageKey, e);
            return ConsumeResult.FAILURE;
        }
    }

    /**
     * 执行互动巡检报告生成。
     *
     * <p>{@code generate()} 内部已 try-catch 全部业务异常 + Token 返还 + 状态补偿，
     * 正常路径下不会抛 BusinessException 出来；这里再 catch 一层兜底防御性。</p>
     *
     * @param report     报告实体
     * @param msg        触发消息
     * @param messageKey 消息 key
     * @return 消费结果
     */
    private ConsumeResult doPatrolGenerate(ScriptMonitorReportEntity report, ScriptMonitorTriggerMsg msg, String messageKey) {
        try {
            patrolGenerateBll.generate(report, msg.getOriginalStatus(), msg.getWithhold());
            return ConsumeResult.SUCCESS;
        } catch (BusinessException e) {
            log.warn("[script-monitor-handler] 互动巡检业务异常 reportId={} messageKey={} code={} msg={}",
                    report.getId(), messageKey, e.getCode(), e.getMessage());
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            log.error("[script-monitor-handler] 互动巡检系统异常 reportId={} messageKey={}",
                    report.getId(), messageKey, e);
            return ConsumeResult.FAILURE;
        }
    }
}
