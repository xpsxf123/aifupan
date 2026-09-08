package com.jiuyu.replay.ai.api;

import com.jiuyu.replay.ai.bll.ScriptMonitorBll;
import com.jiuyu.replay.common.constant.OrderEnums;
import com.jiuyu.replay.generic.enums.words.MonitorTypeEnum;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.StandardScriptFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.generic.vo.words.StandardScriptInfoVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorApi 自动触发 SPI 单测（B7）
 *
 * <p>覆盖 autoTriggerForVideo 的开关路由 / hasAuth 守门 / Token 余额守门 /
 * anchor 不存在静默返回等场景。</p>
 *
 * @author beta
 * @date 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorApiTest {

    @Mock
    private ScriptMonitorBll scriptMonitorBll;

    @Mock
    private UserPropertyFeign userPropertyFeign;

    @Mock
    private AnchorUrlUserFeign anchorUrlUserFeign;

    @Mock
    private StandardScriptFeign standardScriptFeign;

    @InjectMocks
    private ScriptMonitorApi api;

    private static final Long USER_ID = 1L;
    private static final Long TENANT_ID = 100L;
    private static final String VIDEO_ID = "v1";
    private static final String SEC_UID = "sec-001";

    @BeforeEach
    void setUp() {
        // 默认 autoTriggerReport 返回 true
        when(scriptMonitorBll.autoTriggerReport(anyString(), anyLong(), anyLong(), anyInt())).thenReturn(true);
    }

    // ---------- helpers ----------

    /**
     * 构造 anchor_url_user 的三开关数据并 mock 返回
     */
    private AnchorUrlUserVo anchor(int qualityInspection, int fidelityMonitor, int interactionPatrol) {
        AnchorUrlUserVo vo = new AnchorUrlUserVo();
        vo.setIsScriptQualityInspection(qualityInspection);
        vo.setIsScriptFidelityMonitor(fidelityMonitor);
        vo.setIsInteractionPatrol(interactionPatrol);
        when(anchorUrlUserFeign.getBySecUidAndUser(anyString(), anyLong(), anyLong())).thenReturn(R.ok(vo));
        return vo;
    }

    /**
     * mock hasAuth 结果
     */
    private void mockHasAuth(String code, boolean hasAuth) {
        MonitorPositionAuthVo auth = new MonitorPositionAuthVo();
        auth.setHasAuth(hasAuth);
        when(userPropertyFeign.checkMonitorPosition(anyLong(), eq(code))).thenReturn(auth);
    }

    /**
     * mock AI Token 余额
     */
    private void mockTokenBalance(long total, long use) {
        UserPropertyTypeInfoVo v = new UserPropertyTypeInfoVo();
        v.setCommodityTypeCode("aiTokenNum");
        v.setTotalQuantity(total);
        v.setUseQuantity(use);
        when(userPropertyFeign.getUserProperty(anyLong())).thenReturn(List.of(v));
    }

    // ---------- 用例 ----------

    @Test
    @DisplayName("质检开关=1 巡检开关=1 各自独立触发一次")
    void autoTriggerForVideo_qualityAndPatrolBothEnabled_independentlyCalledOnce() {
        // given
        anchor(1, 0, 1); // 质检=1, 还原度=0, 巡检=1
        mockHasAuth(OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode(), true);
        mockHasAuth(OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode(), true);
        mockTokenBalance(200000L, 0L);

        // when
        R<String> result = api.autoTriggerForVideo(VIDEO_ID, USER_ID, TENANT_ID, SEC_UID);

        // then
        assertThat(result.getCode()).isEqualTo(0);
        verify(scriptMonitorBll, times(1)).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        verify(scriptMonitorBll, times(1)).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.INTERACTION_PATROL.getCode());
    }

    @Test
    @DisplayName("三开关全为 0 — 不调任何能力")
    void autoTriggerForVideo_allSwitchesOff_doesNotCallAnyCapability() {
        // given
        anchor(0, 0, 0);

        // when
        R<String> result = api.autoTriggerForVideo(VIDEO_ID, USER_ID, TENANT_ID, SEC_UID);

        // then
        assertThat(result.getCode()).isEqualTo(0);
        verify(scriptMonitorBll, never()).autoTriggerReport(anyString(), anyLong(), anyLong(), anyInt());
        verify(userPropertyFeign, never()).checkMonitorPosition(anyLong(), anyString());
    }

    @Test
    @DisplayName("质检 hasAuth=false 跳过 — 巡检 hasAuth=true 正常触发")
    void autoTriggerForVideo_hasAuthFalse_skip_doesNotAffectOthers() {
        // given
        anchor(1, 0, 1);
        // 质检无授权
        mockHasAuth(OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode(), false);
        // 巡检有授权
        mockHasAuth(OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode(), true);
        mockTokenBalance(200000L, 0L);

        // when
        api.autoTriggerForVideo(VIDEO_ID, USER_ID, TENANT_ID, SEC_UID);

        // then：质检不调 autoTriggerReport
        verify(scriptMonitorBll, never()).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        // 巡检正常调一次
        verify(scriptMonitorBll, times(1)).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.INTERACTION_PATROL.getCode());
    }

    @Test
    @DisplayName("Token 余额 < 100000 — 跳过所有触发")
    void autoTriggerForVideo_tokenInsufficient_skip_warnLog() {
        // given
        anchor(1, 0, 1);
        mockHasAuth(OrderEnums.commodityTypeCode.SCRIPT_QUALITY_NUM.getCode(), true);
        mockHasAuth(OrderEnums.commodityTypeCode.INTERACTION_PATROL_NUM.getCode(), true);
        // Token 不足（balance = 50000 - 0 = 50000 < 100000）
        mockTokenBalance(50000L, 0L);

        // when
        api.autoTriggerForVideo(VIDEO_ID, USER_ID, TENANT_ID, SEC_UID);

        // then
        verify(scriptMonitorBll, never()).autoTriggerReport(anyString(), anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("anchor_url_user 不存在（code≠0 或 data=null）— 静默返回 R.ok")
    void autoTriggerForVideo_anchorUrlUserNotExist_silentlyReturns() {
        // given：Feign 返回数据不存在（code=30000）
        R<AnchorUrlUserVo> notFound = new R<>();
        notFound.setCode(30000);
        when(anchorUrlUserFeign.getBySecUidAndUser(anyString(), anyLong(), anyLong())).thenReturn(notFound);

        // when
        R<String> result = api.autoTriggerForVideo(VIDEO_ID, USER_ID, TENANT_ID, SEC_UID);

        // then：静默返回 R.ok，不调任何能力
        assertThat(result.getCode()).isEqualTo(0);
        verify(scriptMonitorBll, never()).autoTriggerReport(anyString(), anyLong(), anyLong(), anyInt());
    }

    // ---------- Slice B AC-003: 还原度自动触发分支 ----------

    /**
     * 构造有效标准稿 VO（用于 fidelity 路径 mock）。
     */
    private StandardScriptInfoVo standardScript() {
        StandardScriptInfoVo vo = new StandardScriptInfoVo();
        vo.setId(200L);
        vo.setTenantId(TENANT_ID);
        vo.setUserId(USER_ID);
        vo.setSecUid(SEC_UID);
        vo.setSpeechMode(0);
        return vo;
    }

    /**
     * AC-003：isScriptFidelityMonitor=1 + hasAuth=true + Token 充足 + 有标准稿 → 调 autoTriggerReport(monitorType=1) 一次。
     */
    @Test
    @DisplayName("AC-003: 还原度开关=1 + 自有授权 + Token 足 + 有标准稿 → 调 autoTriggerReport(monitorType=1) 一次")
    void autoTriggerForVideo_fidelityEnabled_hasStandardScript_callsAutoTriggerOnce() {
        // given：还原度开关=1，其他全 0
        anchor(0, 1, 0);
        mockHasAuth(OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode(), true);
        mockTokenBalance(200000L, 0L);
        // 关键 mock：StandardScriptFeign 返有效标准稿
        when(standardScriptFeign.findValid(TENANT_ID, USER_ID, SEC_UID)).thenReturn(standardScript());

        // when
        api.autoTriggerForVideo(VIDEO_ID, USER_ID, TENANT_ID, SEC_UID);

        // then：autoTriggerReport(monitorType=1) 被调一次（FIDELITY_MONITOR.code=1）
        verify(scriptMonitorBll, times(1)).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        // 反向断言：质检 / 巡检不被触发（开关=0）
        verify(scriptMonitorBll, never()).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        verify(scriptMonitorBll, never()).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.INTERACTION_PATROL.getCode());
    }

    /**
     * 还原度开关=1 + 无标准稿 → log.warn + skip；不调 autoTriggerReport(monitorType=1)；不阻断其他能力。
     */
    @Test
    @DisplayName("AC-003 反向: 还原度开关=1 但无标准稿 → skip + 不调 autoTriggerReport(monitorType=1)")
    void autoTriggerForVideo_fidelityEnabled_noStandardScript_skipsAutoTrigger() {
        anchor(0, 1, 0);
        mockHasAuth(OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode(), true);
        mockTokenBalance(200000L, 0L);
        // 关键 mock：StandardScriptFeign 返 null
        when(standardScriptFeign.findValid(TENANT_ID, USER_ID, SEC_UID)).thenReturn(null);

        api.autoTriggerForVideo(VIDEO_ID, USER_ID, TENANT_ID, SEC_UID);

        // 关键断言：还原度不被触发（无标准稿）
        verify(scriptMonitorBll, never()).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.FIDELITY_MONITOR.getCode());
    }

    /**
     * 还原度开关=1 + hasAuth=false → skip；不调 StandardScriptFeign（短路）；不调 autoTriggerReport。
     */
    @Test
    @DisplayName("AC-003 短路: 还原度 hasAuth=false → skip + 不调 StandardScriptFeign + 不调 autoTriggerReport")
    void autoTriggerForVideo_fidelityEnabled_noAuth_shortCircuit() {
        anchor(0, 1, 0);
        // 关键 mock：还原度无授权
        mockHasAuth(OrderEnums.commodityTypeCode.SCRIPT_FIDELITY_NUM.getCode(), false);

        api.autoTriggerForVideo(VIDEO_ID, USER_ID, TENANT_ID, SEC_UID);

        verify(scriptMonitorBll, never()).autoTriggerReport(VIDEO_ID, USER_ID, TENANT_ID,
                MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        // 关键短路断言：hasAuth=false 时 StandardScriptFeign 不应被调（节省调用）
        verify(standardScriptFeign, never()).findValid(anyLong(), anyLong(), anyString());
    }
}
