package com.jiuyu.replay.generic.feign.order;

import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;

/**
 * AI Token 预扣/结算/返还 SPI 接口。
 *
 * <p>B4-0 收敛 SPI：将"预扣 → 结算（查余额封顶+实扣+清预扣+记台账）→ 返还"三段编排
 * 收拢到 order 模块唯一实现，消费方（replay-ai 等）只需调用本接口，
 * 禁止再在各业务层自行拼装底层原子操作。</p>
 *
 * <p><b>使用范式：</b></p>
 * <pre>
 *   RedisWithholdVo withhold = aiTokenWithholdFeign.withholdAiToken(userId, 20000L);
 *   try {
 *       // ... AI 调用 ...
 *       aiTokenWithholdFeign.settleAiToken(withhold, userId, aiReturn, recordBo);
 *   } catch (Exception e) {
 *       aiTokenWithholdFeign.returnAiToken(withhold);
 *       throw e;
 *   }
 * </pre>
 *
 * @author beta
 * @date 2026-05-30
 */
public interface AiTokenWithholdFeign {

    /**
     * 预扣 AI Token。
     *
     * <p>余额充足时在 Redis 写入预扣记录并返回预扣凭据；
     * 余额不足时抛出 {@code BusinessException(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH)}。</p>
     *
     * @param userId 用户 ID
     * @param preNum 预扣数量（正整数）
     * @return 预扣凭据（含 {@code withholdId} 和 {@code redisId}），调用方须妥善持有以便后续结算或返还
     * @throws com.jiuyu.replay.generic.vo.common.exception.BusinessException 余额不足时抛出（StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH，code=70001）
     */
    RedisWithholdVo withholdAiToken(Long userId, Long preNum);

    /**
     * 结算 AI Token（实扣 + 清预扣缓存 + 写台账）。
     *
     * <p>实扣数量 = {@code min(aiReturn.totalTokens, 当前账户 aiTokenNum 余额)}；
     * 完成实扣后清除 {@code withhold} 对应的 Redis 预扣记录，并将 {@code recordBo}
     * 补全后写入 AiTokenUseRecord 台账。该方法为原子调用（数据库事务）。</p>
     *
     * @param withhold  预扣凭据（由 {@link #withholdAiToken} 返回）
     * @param userId    用户 ID
     * @param aiReturn  AI 调用结果（含 {@code totalTokens} 实际消耗量）
     * @param recordBo  台账预填参数（调用方填写 useSourceType/useSourceId/assistantType/modelName 等业务字段）
     */
    void settleAiToken(RedisWithholdVo withhold, Long userId, AiReturnDataVo aiReturn, AiTokenUseRecordBo recordBo);

    /**
     * 返还 AI Token（失败时清预扣缓存，不实扣，不写台账）。
     *
     * <p>AI 调用失败时调用，只清除 Redis 预扣记录，不做任何资产扣减和台账写入。</p>
     *
     * @param withhold 预扣凭据（由 {@link #withholdAiToken} 返回）
     */
    void returnAiToken(RedisWithholdVo withhold);
}
