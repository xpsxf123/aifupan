package com.jiuyu.replay.api.service.aiagent;

import com.jiuyu.replay.generic.bo.aiagent.AiAgentBaseBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaReleaseBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaReserveBo;
import com.jiuyu.replay.generic.bo.aiagent.QuotaSettleBo;
import com.jiuyu.replay.generic.vo.aiagent.*;
import com.jiuyu.replay.generic.vo.common.R;

/**
 * AI Agent 算力额度收口：余额查询 / 预扣（预校验）/ 结算 / 释放。
 *
 * <p>底层复用爱复盘既有 aiToken 资产体系（{@code UserPropertyBll} 预扣 / 扣减 / 释放原语），本服务只做
 * 「开放 API 适配 + requestId 幂等去重」。预扣为 Redis 占位（TTL 30min），结算按真实账单 token 扣减并释放预扣，
 * 全部 Redis/缓存级操作、无新增数据库表。</p>
 *
 * @author fupan-server
 */
public interface AiAgentQuotaService {

    /**
     * AI 算力计费系数配置（全局默认倍率 + 缓存倍率 + 各模型消耗倍率）。
     *
     * <p>供 AI Agent 客户端本地复刻服务端计费公式。系数为全局/模型级配置，与身份无关，
     * 入参仅用于鉴权一致性与日志，不参与数据过滤。</p>
     *
     * @param bo 身份
     * @return 计费系数配置
     */
    R<AiCoefficientConfigVo> coefficientConfig(AiAgentBaseBo bo);

    /** 余额查询（热路径外，展示/软校验）。 */
    R<QuotaBalanceVo> balance(AiAgentBaseBo bo);

    /** 预扣 + 余额预校验（热路径）。requestId 幂等：重试返回同一预扣。 */
    R<QuotaReserveVo> reserve(QuotaReserveBo bo);

    /** 结算：按真实账单 token 扣减并释放预扣。requestId 幂等：重复结算不重复扣。 */
    R<QuotaSettleVo> settle(QuotaSettleBo bo);

    /** 释放预扣（取消/报错且未结算时）。天然幂等。 */
    R<QuotaReleaseVo> release(QuotaReleaseBo bo);
}
