package com.jiuyu.replay.order.api;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.vo.ai.AiReturnDataVo;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.RedisWithholdVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.order.bean.impl.UserPropertyImpl;
import com.jiuyu.replay.order.bll.AiTokenUseRecordBll;
import com.jiuyu.replay.order.bll.UserPropertyBll;
import com.jiuyu.replay.order.bo.AssetsMinusOrPlusBo;
import com.jiuyu.replay.order.bo.IsPropertyHaveBo;
import com.jiuyu.replay.order.vo.IsPropertyHaveVo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI Token 预扣/结算/返还 SPI 实现。
 *
 * <p>收敛原本散落在 api 层三处（AiRelatedLogicImpl / UserPropertyLogicImpl / DataScreenshotLogicImpl）
 * 的预扣结算编排，所有新代码统一走本接口，禁止再自行拼装底层原子操作。
 * 存量三处旧编排的迁移见 DEBT-001（独立技术债任务）。</p>
 *
 * <p><b>结算编排逻辑（settleAiToken）：</b>
 * <ol>
 *   <li>查询当前账户 aiTokenNum 实时余额（totalQuantity - useQuantity）</li>
 *   <li>实扣量 = {@code min(aiReturn.totalTokens, 当前余额)}，取负（减法方向）</li>
 *   <li>通过 {@code AssetsMinusOrPlusBo.clearWithholdCache} 回调在实扣前清除 Redis 预扣记录</li>
 *   <li>调用 {@link UserPropertyImpl#use(AssetsMinusOrPlusBo)} 执行实扣</li>
 *   <li>调用 {@link AiTokenUseRecordBll#save(AiTokenUseRecordBo)} 写台账</li>
 * </ol>
 * </p>
 *
 * @author beta
 * @date 2026-05-30
 * @see AiTokenWithholdFeign
 */
@Component
@AllArgsConstructor
@Slf4j
public class AiTokenWithholdApi implements AiTokenWithholdFeign {

    /**
     * AI Token 资产 code，对应 tb_commodity_type.code 字段。
     */
    private static final String AI_TOKEN_CODE = "aiTokenNum";

    private final UserPropertyBll userPropertyBll;
    private final AiTokenUseRecordBll aiTokenUseRecordBll;

    /**
     * {@inheritDoc}
     *
     * <p>内部调用 {@link UserPropertyBll#isPropertyHaveAiToken(IsPropertyHaveBo)} 完成预扣原子操作。
     * 若 {@code isHave=false}（余额不足）则抛出 BusinessException(70001)。</p>
     *
     * @param userId 用户 ID
     * @param preNum 预扣数量（正整数）
     * @return 预扣凭据（含 withholdId / redisId，调用方持有用于后续 settle 或 return）
     * @throws BusinessException 余额不足时，code = 70001（SCRIPT_MONITOR_TOKEN_NOT_ENOUGH）
     */
    @Override
    public RedisWithholdVo withholdAiToken(Long userId, Long preNum) {
        IsPropertyHaveBo bo = new IsPropertyHaveBo();
        bo.setUserId(userId);
        bo.setCode(AI_TOKEN_CODE);
        bo.setThisUseNum(preNum);

        R<IsPropertyHaveVo> result = userPropertyBll.isPropertyHaveAiToken(bo);
        if (result.getCode() != 0) {
            log.warn("预扣 AI Token 调用底层异常，userId={}, preNum={}, msg={}", userId, preNum, result.getMsg());
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH);
        }

        IsPropertyHaveVo vo = result.getData();
        if (vo == null || !Boolean.TRUE.equals(vo.getIsHave())) {
            log.info("用户 {} AI Token 余额不足，预扣 {} 失败", userId, preNum);
            throw new BusinessException(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH);
        }

        // 将 IsPropertyHaveVo 的关键凭据映射为 RedisWithholdVo（作为跨模块传递的凭据）
        RedisWithholdVo withhold = new RedisWithholdVo();
        withhold.setWithholdId(vo.getWithholdId());
        withhold.setRedisId(vo.getRedisId());
        withhold.setCommodityTypeCode(AI_TOKEN_CODE);
        log.info("用户 {} AI Token 预扣成功，withholdId={}", userId, vo.getWithholdId());
        return withhold;
    }

    /**
     * {@inheritDoc}
     *
     * <p>结算流程：查当前余额 → 按余额封顶计算实扣量 → 设 clearWithholdCache 回调 → 实扣 → 写台账。</p>
     *
     * @param withhold  预扣凭据
     * @param userId    用户 ID
     * @param aiReturn  AI 返回结果（含 totalTokens）
     * @param recordBo  台账业务参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleAiToken(RedisWithholdVo withhold, Long userId, AiReturnDataVo aiReturn, AiTokenUseRecordBo recordBo) {
        if (ObjectUtil.isNull(withhold) || ObjectUtil.isNull(userId) || ObjectUtil.isNull(aiReturn)) {
            log.warn("settleAiToken 参数不合法，withhold={}, userId={}, aiReturn={}", withhold, userId, aiReturn);
            return;
        }

        // 查询当前账户实时余额
        UserPropertyTypeInfoVo propertyInfo = userPropertyBll.getUserPropertyByCode(userId, AI_TOKEN_CODE);
        long currentBalance = 0L;
        if (ObjectUtil.isNotNull(propertyInfo)) {
            long total = ObjectUtil.defaultIfNull(propertyInfo.getTotalQuantity(), 0L);
            long used = ObjectUtil.defaultIfNull(propertyInfo.getUseQuantity(), 0L);
            currentBalance = Math.max(total - used, 0L);
        }

        // 实扣量 = min(totalTokens, 当前余额)
        long actualTokens = ObjectUtil.defaultIfNull(aiReturn.getTotalTokens(), 0);
        long deductNum = Math.min(actualTokens, currentBalance);

        // MINOR-4 修补（2026-06-01）：deductNum=0 时无需 UserPropertyImpl.use（避免写 tokenUsed=0 的无效台账），仅清预扣缓存即可
        if (deductNum == 0) {
            userPropertyBll.removeTempUserProperty(withhold.getWithholdId(), withhold.getRedisId());
            log.info("settleAiToken: deductNum=0 (余额={}, 应扣={})，跳过扣减+台账，仅清预扣 withholdId={}",
                    currentBalance, actualTokens, withhold.getWithholdId());
            return;
        }

        // 构造实扣参数
        AssetsMinusOrPlusBo assets = new AssetsMinusOrPlusBo();
        assets.setUserId(userId);
        // user_name 列 NOT NULL，必须赋值过约束；该列已不再用于业务查询（历史搜索功能将逐步下线），故填空串
        assets.setUserName("");
        assets.setCode(AI_TOKEN_CODE);
        assets.setNum(-deductNum);
        assets.setWithholdId(withhold.getWithholdId());
        assets.setRedisId(withhold.getRedisId());
        if (deductNum < actualTokens) {
            assets.setRemarks(StrUtil.format("本次扣款：{}，但当前 AI Token 余额为：{}，本次全部扣完", actualTokens, currentBalance));
        }
        // 实扣前清除预扣缓存（通过回调在 UserPropertyImpl.use 中调用）
        assets.setClearWithholdCache(userPropertyBll::removeTempUserProperty);

        // 执行实扣
        UserPropertyImpl.use(assets);
        log.info("用户 {} AI Token 实扣完成，deductNum={}, withholdId={}", userId, deductNum, withhold.getWithholdId());

        // 写台账
        if (ObjectUtil.isNotNull(recordBo)) {
            recordBo.setUserId(userId);
            aiTokenUseRecordBll.save(recordBo);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>只清除 Redis 预扣记录，不执行任何资产扣减和台账写入。</p>
     *
     * @param withhold 预扣凭据
     */
    @Override
    public void returnAiToken(RedisWithholdVo withhold) {
        if (ObjectUtil.isNull(withhold)) {
            log.warn("returnAiToken 收到 null 凭据，忽略");
            return;
        }
        userPropertyBll.removeTempUserProperty(withhold.getWithholdId(), withhold.getRedisId());
        log.info("AI Token 预扣返还完成，withholdId={}", withhold.getWithholdId());
    }
}
