package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.service.aiagent.AiAgentQuotaService;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.generic.bo.aiagent.AiAgentBaseBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaReleaseBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaReserveBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaSettleBo;
import com.jiuyu.replay.generic.vo.aiagent.*;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI Agent 算力额度收口开放接口（API-Key 鉴权，身份在请求体）。
 *
 * <p>供 AI Agent 项目按「预扣 → 事后结算 → 失败补偿」可靠扣减算力：reserve 热路径预扣 + 余额预校验、settle 事后按真实
 * 账单 token 结算（幂等）、release 取消/报错时释放预扣（幂等）、balance 余额查询。底层复用爱复盘既有 aiToken 资产体系，
 * 与既有 ask 流扣减同口径，全部 Redis/缓存级操作。</p>
 *
 * @author fupan-server
 */
@Slf4j
@Validated
@RestController
@APIKey
@AllArgsConstructor
@RequestMapping("/internal/ai-agent/ai/quota")
@Tag(name = "AI Agent 算力额度收口接口")
public class AiAgentQuotaController {

    private static final String LOG_PREFIX = "[AI-AGENT][QUOTA]";

    private final AiAgentQuotaService aiAgentQuotaService;


    /**
     * AI 算力计费系数配置（全局默认倍率 + 缓存倍率 + 各模型消耗倍率）。
     *
     * <p>供 AI Agent 客户端本地复刻服务端计费公式。系数为全局/模型级配置，与身份无关。</p>
     *
     * @param bo 身份
     * @return 计费系数配置
     */
    @PostMapping("/coefficient")
    @Operation(summary = "AI 计费系数配置")
    public R<AiCoefficientConfigVo> coefficientConfig(@Valid @RequestBody AiAgentBaseBo bo) {
        log.info("{} ai/coefficient, tenantId={}, userType={}", LOG_PREFIX, bo.getTenantId(), bo.getUserType());
        return aiAgentQuotaService.coefficientConfig(bo);
    }

    /**
     * 余额查询（热路径外，展示/软校验）。
     *
     * @param bo 身份
     * @return 总额度 / 已用 / 可用
     */
    @PostMapping("/balance")
    @Operation(summary = "算力额度余额查询")
    public R<QuotaBalanceVo> balance(@Valid @RequestBody AiAgentBaseBo bo) {
        log.info("{} balance, tenantId={}, userId={}", LOG_PREFIX, bo.getTenantId(), bo.getUserId());
        return aiAgentQuotaService.balance(bo);
    }

    /**
     * 预扣 + 余额预校验（热路径，对话开跑前调用）。requestId 幂等。
     *
     * @param bo 身份 + requestId + 预估 token
     * @return 是否放行 + 预扣令牌 reserveId + 可用额度
     */
    @PostMapping("/reserve")
    @Operation(summary = "算力额度预扣（预校验）")
    public R<QuotaReserveVo> reserve(@Valid @RequestBody QuotaReserveBo bo) {
        log.info("{} reserve, tenantId={}, userId={}, requestId={}, estimate={}",
                LOG_PREFIX, bo.getTenantId(), bo.getUserId(), bo.getRequestId(), bo.getEstimateTokens());
        return aiAgentQuotaService.reserve(bo);
    }

    /**
     * 结算（本轮结束后按真实账单 token 扣减并释放预扣）。requestId 幂等：重复结算不重复扣。
     *
     * @param bo 身份 + requestId + reserveId + 真实账单 token
     * @return 是否已结算 + 实际扣减 + 扣减后余额 + 是否幂等命中
     */
    @PostMapping("/settle")
    @Operation(summary = "算力额度结算")
    public R<QuotaSettleVo> settle(@Valid @RequestBody QuotaSettleBo bo) {
        log.info("{} settle, tenantId={}, userId={}, requestId={}, billed={}",
                LOG_PREFIX, bo.getTenantId(), bo.getUserId(), bo.getRequestId(), bo.getBilledTokens());
        return aiAgentQuotaService.settle(bo);
    }

    /**
     * 释放预扣（取消/报错且未结算时）。幂等。
     *
     * @param bo 身份 + requestId + reserveId
     * @return 是否已释放
     */
    @PostMapping("/release")
    @Operation(summary = "算力额度预扣释放")
    public R<QuotaReleaseVo> release(@Valid @RequestBody QuotaReleaseBo bo) {
        log.info("{} release, tenantId={}, userId={}, requestId={}",
                LOG_PREFIX, bo.getTenantId(), bo.getUserId(), bo.getRequestId());
        return aiAgentQuotaService.release(bo);
    }


}
