package com.jiuyu.replay.order.api;

import com.jiuyu.replay.generic.bo.third.AiTokenUseRecordBo;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiTokenWithholdApi 单测（B4-0 预扣/结算/返还 SPI）
 *
 * <p>全部使用 Mockito，无需 Spring 上下文（{@link UserPropertyImpl#use} 静态方法通过
 * {@code MockedStatic} 隔离）。</p>
 *
 * @author beta
 * @date 2026-05-30
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiTokenWithholdApiTest {

    @Mock
    private UserPropertyBll userPropertyBll;

    @Mock
    private AiTokenUseRecordBll aiTokenUseRecordBll;

    @InjectMocks
    private AiTokenWithholdApi api;

    // ---------- 辅助方法 ----------

    /**
     * 构造预扣成功的 IsPropertyHaveVo。
     */
    private IsPropertyHaveVo haveVo(String withholdId, Long redisId) {
        IsPropertyHaveVo vo = new IsPropertyHaveVo();
        vo.setIsHave(true);
        vo.setWithholdId(withholdId);
        vo.setRedisId(redisId);
        return vo;
    }

    /**
     * 构造余额不足的 IsPropertyHaveVo（isHave=false）。
     */
    private IsPropertyHaveVo notHaveVo() {
        IsPropertyHaveVo vo = new IsPropertyHaveVo();
        vo.setIsHave(false);
        return vo;
    }

    /**
     * 构造用户资产 VO（aiTokenNum）。
     */
    private UserPropertyTypeInfoVo tokenInfo(long total, long used) {
        UserPropertyTypeInfoVo info = new UserPropertyTypeInfoVo();
        info.setCommodityTypeCode("aiTokenNum");
        info.setTotalQuantity(total);
        info.setUseQuantity(used);
        return info;
    }

    /**
     * 构造预扣凭据 RedisWithholdVo。
     */
    private RedisWithholdVo withhold(String withholdId, Long redisId) {
        RedisWithholdVo wh = new RedisWithholdVo();
        wh.setWithholdId(withholdId);
        wh.setRedisId(redisId);
        wh.setCommodityTypeCode("aiTokenNum");
        return wh;
    }

    /**
     * 构造 AiReturnDataVo（含 totalTokens）。
     */
    private AiReturnDataVo aiReturn(int totalTokens) {
        AiReturnDataVo r = new AiReturnDataVo();
        r.setTotalTokens(totalTokens);
        return r;
    }

    // ========== AC-1：余额充足 → withholdAiToken 返回非空凭据 ==========

    /**
     * AC-1：Token 余额 >= preNum，withholdAiToken 返回非空 RedisWithholdVo（含 withholdId/redisId）。
     */
    @Test
    void withholdAiToken_balanceSufficient_returnsNonNullWithholdVo() {
        // Given
        long userId = 1001L;
        long preNum = 20000L;
        IsPropertyHaveVo haveVo = haveVo("1001_888", 888L);
        when(userPropertyBll.isPropertyHaveAiToken(any(IsPropertyHaveBo.class)))
                .thenReturn(R.ok(haveVo));

        // When
        RedisWithholdVo result = api.withholdAiToken(userId, preNum);

        // Then
        assertNotNull(result);
        assertEquals("1001_888", result.getWithholdId());
        assertEquals(888L, result.getRedisId());
        assertEquals("aiTokenNum", result.getCommodityTypeCode());
    }

    /**
     * AC-1 补充：withholdAiToken 调用 UserPropertyBll 时传入正确的 userId/code/preNum。
     */
    @Test
    void withholdAiToken_passesCorrectBoToUserPropertyBll() {
        // Given
        long userId = 2002L;
        long preNum = 10000L;
        IsPropertyHaveVo haveVo = haveVo("2002_999", 999L);
        ArgumentCaptor<IsPropertyHaveBo> captor = ArgumentCaptor.forClass(IsPropertyHaveBo.class);
        when(userPropertyBll.isPropertyHaveAiToken(captor.capture())).thenReturn(R.ok(haveVo));

        // When
        api.withholdAiToken(userId, preNum);

        // Then
        IsPropertyHaveBo captured = captor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals("aiTokenNum", captured.getCode());
        assertEquals(preNum, captured.getThisUseNum());
    }

    // ========== AC-2：余额不足 → 抛 BusinessException 70001 ==========

    /**
     * AC-2：Token 余额 < preNum（isHave=false），抛出 BusinessException code=70001。
     */
    @Test
    void withholdAiToken_balanceInsufficient_throwsBusinessException70001() {
        // Given
        long userId = 3003L;
        long preNum = 20000L;
        when(userPropertyBll.isPropertyHaveAiToken(any(IsPropertyHaveBo.class)))
                .thenReturn(R.ok(notHaveVo()));

        // When / Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> api.withholdAiToken(userId, preNum));
        assertEquals(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getCode(), ex.getCode());
    }

    /**
     * AC-2 补充：底层 BLL 返回 code != 0 时也抛 70001。
     */
    @Test
    void withholdAiToken_bllReturnsError_throwsBusinessException70001() {
        // Given
        when(userPropertyBll.isPropertyHaveAiToken(any(IsPropertyHaveBo.class)))
                .thenReturn(R.error(3001, "资产code为空"));

        // When / Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> api.withholdAiToken(4004L, 20000L));
        assertEquals(StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getCode(), ex.getCode());
    }

    // ========== AC-3：settleAiToken 按 min(totalTokens, 余额) 实扣 + 清预扣缓存 + 台账落库 ==========

    /**
     * AC-3（主路径）：totalTokens <= 当前余额 → 实扣量 = totalTokens，清预扣缓存，AiTokenUseRecord 落库。
     */
    @Test
    void settleAiToken_tokensBelowBalance_deductsActualTokens() {
        // Given
        long userId = 5005L;
        long totalTokens = 5000L;
        long currentBalance = 100000L; // 10万余额 > 5000
        RedisWithholdVo wh = withhold("5005_111", 111L);
        AiReturnDataVo aiReturn = aiReturn((int) totalTokens);
        AiTokenUseRecordBo recordBo = new AiTokenUseRecordBo();
        recordBo.setModelName("doubao-pro-32k");

        when(userPropertyBll.getUserPropertyByCode(userId, "aiTokenNum"))
                .thenReturn(tokenInfo(currentBalance, 0L));

        ArgumentCaptor<AssetsMinusOrPlusBo> assetsCaptor = ArgumentCaptor.forClass(AssetsMinusOrPlusBo.class);

        try (MockedStatic<UserPropertyImpl> staticMock = mockStatic(UserPropertyImpl.class)) {
            staticMock.when(() -> UserPropertyImpl.use(assetsCaptor.capture())).thenAnswer(inv -> null);

            // When
            api.settleAiToken(wh, userId, aiReturn, recordBo);

            // Then：实扣量 = -5000
            staticMock.verify(() -> UserPropertyImpl.use(any(AssetsMinusOrPlusBo.class)));
            AssetsMinusOrPlusBo captured = assetsCaptor.getValue();
            assertEquals(-totalTokens, captured.getNum());
            assertEquals(userId, captured.getUserId());
            assertEquals("aiTokenNum", captured.getCode());
            assertNotNull(captured.getClearWithholdCache()); // 清预扣回调已设置

            // 台账落库
            verify(aiTokenUseRecordBll).save(recordBo);
            assertEquals(userId, recordBo.getUserId());
        }
    }

    /**
     * AC-3（封顶路径）：totalTokens > 当前余额 → 实扣量 = 当前余额（不超扣）。
     */
    @Test
    void settleAiToken_tokensExceedBalance_capsDeductAtBalance() {
        // Given
        long userId = 6006L;
        long totalTokens = 50000L;
        long currentBalance = 3000L; // 余额 3000 < totalTokens 50000
        RedisWithholdVo wh = withhold("6006_222", 222L);
        AiReturnDataVo aiReturn = aiReturn((int) totalTokens);
        AiTokenUseRecordBo recordBo = new AiTokenUseRecordBo();

        when(userPropertyBll.getUserPropertyByCode(userId, "aiTokenNum"))
                .thenReturn(tokenInfo(currentBalance + 1000L, 1000L)); // total=4000, used=1000 → balance=3000

        ArgumentCaptor<AssetsMinusOrPlusBo> assetsCaptor = ArgumentCaptor.forClass(AssetsMinusOrPlusBo.class);

        try (MockedStatic<UserPropertyImpl> staticMock = mockStatic(UserPropertyImpl.class)) {
            staticMock.when(() -> UserPropertyImpl.use(assetsCaptor.capture())).thenAnswer(inv -> null);

            // When
            api.settleAiToken(wh, userId, aiReturn, recordBo);

            // Then：实扣量 = -3000（封顶）
            AssetsMinusOrPlusBo captured = assetsCaptor.getValue();
            assertEquals(-currentBalance, captured.getNum());
        }
    }

    // ========== AC-4：returnAiToken → 清预扣缓存，不实扣，不写台账 ==========

    /**
     * AC-4：returnAiToken 仅清预扣缓存，不调用 UserPropertyImpl.use，不写台账。
     */
    @Test
    void returnAiToken_clearsWithholdCacheOnly_noDeductNoBilling() {
        // Given
        RedisWithholdVo wh = withhold("7007_333", 333L);

        // When
        api.returnAiToken(wh);

        // Then：调用 removeTempUserProperty 清预扣缓存
        verify(userPropertyBll).removeTempUserProperty("7007_333", 333L);
        // 不调用实扣
        verify(aiTokenUseRecordBll, never()).save(any());
    }

    /**
     * AC-4 补充：returnAiToken 不触发任何资产扣减（无 UserPropertyImpl.use 调用）。
     */
    @Test
    void returnAiToken_doesNotDeductAssets() {
        // Given
        RedisWithholdVo wh = withhold("8008_444", 444L);

        try (MockedStatic<UserPropertyImpl> staticMock = mockStatic(UserPropertyImpl.class)) {
            // When
            api.returnAiToken(wh);

            // Then：不调用 UserPropertyImpl.use
            staticMock.verify(() -> UserPropertyImpl.use(any()), never());
        }
    }
}
