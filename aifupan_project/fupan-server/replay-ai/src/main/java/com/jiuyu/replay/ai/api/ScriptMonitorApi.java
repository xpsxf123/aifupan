package com.jiuyu.replay.ai.api;

import cn.hutool.core.collection.CollUtil;
import com.jiuyu.replay.ai.bll.ScriptMonitorBll;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.generic.enums.words.MonitorTypeEnum;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.ScriptMonitorFeign;
import com.jiuyu.replay.generic.feign.words.StandardScriptFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.StandardScriptInfoVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 监控报告自动触发 SPI 实现（B7）
 *
 * <p>由 replay-api 的 AnchorVideoLogicImpl 录制完成（status=2）时同步调用。
 * 内部按 secUid 查三开关 → 逐能力校验 hasAuth + Token → 调 Bll 创建报告 + 投递 MQ。
 * 任一能力失败不阻断其他能力；整体不抛异常（由调用方 catch + log.error 兜底）。</p>
 *
 * @author beta
 * @date 2026-06-02
 */
@Slf4j
@Service
@AllArgsConstructor
public class ScriptMonitorApi implements ScriptMonitorFeign {

    /**
     * AI Token 余额门槛（自动触发）
     */
    private static final long AI_TOKEN_THRESHOLD = 100_000L;
    /**
     * AI Token 资产 code
     */
    private static final String AI_TOKEN_CODE = "aiTokenNum";

    private final ScriptMonitorBll scriptMonitorBll;
    private final UserPropertyFeign userPropertyFeign;
    private final AnchorUrlUserFeign anchorUrlUserFeign;
    private final StandardScriptFeign standardScriptFeign;

    /**
     * 自动触发话术质检 / 互动巡检报告生成（B7）
     *
     * @param videoId  视频 ID（来自 tb_anchor_video）
     * @param userId   用户 ID（视频归属）
     * @param tenantId 租户 ID
     * @param secUid   主播唯一标识（用于查 anchor_url_user 三开关）
     * @return R&lt;String&gt; code=0 表示处理完成（调用方关注 exception 而非 body）
     */
    @Override
    public R<String> autoTriggerForVideo(String videoId, Long userId, Long tenantId, String secUid) {
        log.info("[B7 自动触发] 开始 videoId={} userId={} tenantId={} secUid={}", videoId, userId, tenantId, secUid);

        // 1. 反查 anchor_url_user（含三开关字段）
        R<AnchorUrlUserVo> anchorR = anchorUrlUserFeign.getBySecUidAndUser(secUid, userId, tenantId);
        if (anchorR.getCode() != 0 || anchorR.getData() == null) {
            log.warn("[B7 自动触发] anchor_url_user 不存在，跳过所有能力 secUid={} userId={}", secUid, userId);
            return R.ok("anchor not found, skipped");
        }
        AnchorUrlUserVo anchor = anchorR.getData();

        // 2. 还原度（monitorType=1）
        if (Integer.valueOf(1).equals(anchor.getIsScriptFidelityMonitor())) {
            tryTriggerFidelityCapability(videoId, userId, tenantId, secUid);
        }

        // 3. 质检（monitorType=0）
        if (Integer.valueOf(1).equals(anchor.getIsScriptQualityInspection())) {
            tryTriggerCapability(videoId, userId, tenantId,
                    MonitorTypeEnum.QUALITY_INSPECTION.getCode(),
                    OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode());
        }

        // 4. 巡检（monitorType=2）
        if (Integer.valueOf(1).equals(anchor.getIsInteractionPatrol())) {
            tryTriggerCapability(videoId, userId, tenantId,
                    MonitorTypeEnum.INTERACTION_PATROL.getCode(),
                    OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode());
        }

        return R.ok("auto trigger done");
    }

    /**
     * 单能力触发：hasAuth 校验 + Token 余额校验 + 调 Bll 建报告投 MQ。
     * 失败只打 warn/error log，不抛异常，不阻断其他能力。
     *
     * @param videoId     视频 ID
     * @param userId      用户 ID
     * @param tenantId    租户 ID
     * @param monitorType 监控类型（0=质检 / 2=巡检）
     * @param code        监控位资产 code（scriptQualityNum / interactionPatrolNum）
     */
    private void tryTriggerCapability(String videoId, Long userId, Long tenantId,
                                      Integer monitorType, String code) {
        try {
            // a. 监控位授权量校验（hasAuth=totalQuantity>0）
            MonitorPositionAuthVo auth = userPropertyFeign.checkMonitorPosition(userId, code);
            if (auth == null || !Boolean.TRUE.equals(auth.getHasAuth())) {
                log.warn("[B7 自动触发] hasAuth=false，跳过 videoId={} monitorType={} code={}", videoId, monitorType, code);
                return;
            }

            // b. AI Token 余额校验（≥ 100000 才触发）
            long balance = aiTokenBalance(userId);
            if (balance < AI_TOKEN_THRESHOLD) {
                log.warn("[B7 自动触发] Token 不足，跳过 videoId={} monitorType={} balance={}", videoId, monitorType, balance);
                return;
            }

            // c. 调 Bll 创建报告 + 投 MQ
            boolean ok = scriptMonitorBll.autoTriggerReport(videoId, userId, tenantId, monitorType);
            if (!ok) {
                log.warn("[B7 自动触发] autoTriggerReport 返回 false videoId={} monitorType={}", videoId, monitorType);
            }
        } catch (Exception e) {
            log.error("[B7 自动触发] 单能力异常 videoId={} monitorType={}", videoId, monitorType, e);
            // 不抛，不阻断其他能力
        }
    }

    /**
     * 还原度单能力触发：监控位授权量校验 + Token 余额校验 + 标准稿校验 + 调 Bll 建报告投 MQ。
     *
     * <p>失败只打 warn/error log，不抛异常，不阻断其他能力（mirror tryTriggerCapability 范式）。</p>
     *
     * @param videoId  视频 ID
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @param secUid   主播唯一标识
     */
    private void tryTriggerFidelityCapability(String videoId, Long userId, Long tenantId, String secUid) {
        try {
            // a. 监控位授权量校验（SCRIPT_FIDELITY_NUM）
            MonitorPositionAuthVo auth = userPropertyFeign.checkMonitorPosition(
                    userId, OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode());
            if (auth == null || !Boolean.TRUE.equals(auth.getHasAuth())) {
                log.warn("[B7 自动触发-还原度] hasAuth=false，跳过 videoId={} code={}",
                        videoId, OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode());
                return;
            }

            // b. AI Token 余额校验（≥ 100000 才触发）
            long balance = aiTokenBalance(userId);
            if (balance < AI_TOKEN_THRESHOLD) {
                log.warn("[B7 自动触发-还原度] Token 不足，跳过 videoId={} balance={}", videoId, balance);
                return;
            }

            // c. 标准稿校验（按 tenantId+userId+secUid 有有效标准稿才触发）
            StandardScriptInfoVo standardScript = standardScriptFeign.findValid(tenantId, userId, secUid);
            if (standardScript == null) {
                log.warn("[B7 自动触发-还原度] 无有效标准稿，跳过 videoId={} secUid={}", videoId, secUid);
                return;
            }

            // d. 调 Bll 创建报告 + 投 MQ
            boolean ok = scriptMonitorBll.autoTriggerReport(videoId, userId, tenantId,
                    MonitorTypeEnum.FIDELITY_MONITOR.getCode());
            if (!ok) {
                log.warn("[B7 自动触发-还原度] autoTriggerReport 返回 false videoId={}", videoId);
            }
        } catch (Exception e) {
            log.error("[B7 自动触发-还原度] 单能力异常 videoId={}", videoId, e);
            // 不抛，不阻断其他能力
        }
    }

    /**
     * 查询用户 AI Token 余额（totalQuantity - useQuantity）
     *
     * @param userId 用户 ID
     * @return AI Token 余额（不足或查询失败返回 0）
     */
    private long aiTokenBalance(Long userId) {
        List<UserPropertyTypeInfoVo> props = userPropertyFeign.getUserProperty(userId);
        if (CollUtil.isEmpty(props)) {
            return 0L;
        }
        return props.stream()
                .filter(p -> AI_TOKEN_CODE.equals(p.getCommodityTypeCode()))
                .findAny()
                .map(p -> Math.max(0L, safeLong(p.getTotalQuantity()) - safeLong(p.getUseQuantity())))
                .orElse(0L);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
