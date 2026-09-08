package com.jiuyu.replay.generic.feign.words;

import com.jiuyu.replay.generic.vo.common.R;

/**
 * AI 监控报告自动触发 SPI 接口（B7）
 *
 * <p>声明在 replay-generic，实现在 replay-ai 的 {@code ScriptMonitorApi}。
 * replay-api 的 {@code AnchorVideoLogicImpl} 持有此接口引用，录制完成（status=2）时同步调用。</p>
 *
 * @author beta
 * @date 2026-06-02
 */
public interface ScriptMonitorFeign {

    /**
     * 自动触发话术质检 / 互动巡检报告生成（B7 — 由 AnchorVideoLogicImpl 录制完成时调用）。
     *
     * <p>SPI 内部完成：查 anchor_url_user 三开关 → 逐能力 checkMonitorPosition + Token 校验
     * → 建报告（trigger_source='auto'）+ 直发 MQ。
     * 任一能力失败不阻断其他能力。整体异常由调用方 catch + log.error，不影响主流程。</p>
     *
     * @param videoId  视频 ID（来自 tb_anchor_video）
     * @param userId   用户 ID（视频归属）
     * @param tenantId 租户 ID
     * @param secUid   主播唯一标识（用于查 anchor_url_user 三开关）
     * @return 触发结果（code=0 表示处理完成，调用方关注 exception 而非 body）
     */
    R<String> autoTriggerForVideo(String videoId, Long userId, Long tenantId, String secUid);
}
