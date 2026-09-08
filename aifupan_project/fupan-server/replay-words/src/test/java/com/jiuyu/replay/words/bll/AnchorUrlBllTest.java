package com.jiuyu.replay.words.bll;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlInfoVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.words.bo.anchor.AddOrUpdateAnchorBo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.StandardScriptEntity;
import com.jiuyu.replay.words.producer.AnchorUrlProducer;
import com.jiuyu.replay.words.producer.AnchorUrlUserProducer;
import com.jiuyu.replay.words.producer.BasicSettingsProducer;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.repository.service.StandardScriptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnchorUrlBll 单测（B6 话术智能监控开关校验逻辑）
 *
 * <p>测试范围：addOrUpdateAnchor 中的 Step 1-5 保护逻辑，全部用 Mockito 纯单测，
 * 不启动 Spring 容器，不调用真实 DB / Feign。</p>
 *
 * @author beta
 * @date 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnchorUrlBllTest {

    // -------- 依赖 Mock --------

    @Mock
    private AnchorUrlProducer anchorUrlProducer;
    @Mock
    private AnchorUrlUserProducer anchorUrlUserProducer;
    @Mock
    private AnchorUrlUserService anchorUrlUserService;
    @Mock
    private UserPropertyFeign userPropertyFeign;
    @Mock
    private BasicSettingsProducer basicSettingsProducer;
    @Mock
    private StandardScriptService standardScriptService;

    // 其他字段由 @InjectMocks 自动注入为 null，不影响被测路径
    @InjectMocks
    private AnchorUrlBll anchorUrlBll;

    // -------- helpers --------

    /**
     * 构造一个已存在的 AnchorUrlInfoVo（避免走 save 新增路径）
     */
    private AnchorUrlInfoVo existingAnchorInfo() {
        AnchorUrlInfoVo vo = new AnchorUrlInfoVo();
        vo.setId(10L);
        vo.setSecUid("test-sec-uid");
        vo.setAnchorName("测试主播");
        vo.setAnchorNumber("test-anchor-number");
        vo.setAnchorAvatar("http://avatar.test/1.jpg");
        return vo;
    }

    /**
     * 构造一个已存在的 AnchorUrlUserVo（getUserAnchorBySecUid 返回非 null 时，代表已绑定主播）
     */
    private AnchorUrlUserVo existingUserAnchor() {
        AnchorUrlUserVo vo = new AnchorUrlUserVo();
        vo.setId(20L);
        vo.setAnchorUrlSecUid("test-sec-uid");
        vo.setFolderName("folder");
        vo.setRemarksName("remarks");
        return vo;
    }

    /**
     * 构造 AnchorUrlUserEntity（currentAnchorUser，getOne 查询结果）
     *
     * @param accountType              账号归属类型
     * @param isScriptQualityInspection 话术质检开关
     * @param isScriptFidelityMonitor   话术还原度开关
     * @param isInteractionPatrol       互动巡检开关
     */
    private AnchorUrlUserEntity currentEntity(Integer accountType,
                                               Integer isScriptQualityInspection,
                                               Integer isScriptFidelityMonitor,
                                               Integer isInteractionPatrol) {
        AnchorUrlUserEntity entity = new AnchorUrlUserEntity();
        entity.setId(20L);
        entity.setAccountType(accountType);
        entity.setIsScriptQualityInspection(isScriptQualityInspection);
        entity.setIsScriptFidelityMonitor(isScriptFidelityMonitor);
        entity.setIsInteractionPatrol(isInteractionPatrol);
        return entity;
    }

    /**
     * 构造一个基础请求 Bo
     */
    private AddOrUpdateAnchorBo baseBo() {
        AddOrUpdateAnchorBo bo = new AddOrUpdateAnchorBo();
        bo.setSecUid("test-sec-uid");
        bo.setUserId(1L);
        bo.setTenantId(100L);
        bo.setAnchorName("测试主播");
        bo.setAnchorAvatar("http://avatar.test/1.jpg");
        bo.setAnchorNumber("test-anchor-number");
        bo.setLiveUrl("http://live.test/test-anchor-number");
        return bo;
    }

    /**
     * 构造 Token 余额充足的资产列表（totalQuantity - useQuantity >= 100000）
     */
    private List<UserPropertyTypeInfoVo> sufficientTokenProps() {
        UserPropertyTypeInfoVo v = new UserPropertyTypeInfoVo();
        v.setCommodityTypeCode("aiTokenNum");
        v.setTotalQuantity(200000L);
        v.setUseQuantity(0L);
        return List.of(v);
    }

    /**
     * 构造 Token 余额不足的资产列表（totalQuantity - useQuantity < 100000）
     */
    private List<UserPropertyTypeInfoVo> insufficientTokenProps() {
        UserPropertyTypeInfoVo v = new UserPropertyTypeInfoVo();
        v.setCommodityTypeCode("aiTokenNum");
        v.setTotalQuantity(50000L);
        v.setUseQuantity(0L);
        return List.of(v);
    }

    /**
     * 构造监控位充足的 MonitorPositionAuthVo
     */
    private MonitorPositionAuthVo authWithSurplus() {
        MonitorPositionAuthVo vo = new MonitorPositionAuthVo();
        vo.setHasSurplus(true);
        return vo;
    }

    /**
     * 构造监控位不足的 MonitorPositionAuthVo
     */
    private MonitorPositionAuthVo authNoSurplus() {
        MonitorPositionAuthVo vo = new MonitorPositionAuthVo();
        vo.setHasSurplus(false);
        return vo;
    }

    /**
     * 设置主播和用户主播信息已存在的公共 mock（避免走 save 新增路径）
     */
    private void mockExistingAnchorAndUserAnchor() {
        when(anchorUrlProducer.infoBySecUid("test-sec-uid")).thenReturn(existingAnchorInfo());
        when(anchorUrlUserProducer.getUserAnchorBySecUid("test-sec-uid", 1L, 100L))
                .thenReturn(existingUserAnchor());
    }

    // -------- Step 1：账号切换保护 --------

    @Test
    @DisplayName("AC-001: accountType 从 0 改为非 0，且质检开关=1 时，抛 BusinessException(70007)")
    void addOrUpdateAnchor_accountSwitchProtection_throws70007() {
        // given：主播信息已存在；已绑定；DB 中 accountType=0 + 质检=1
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 1, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setAccountType(1); // 从 0 改为非 0

        // when + then
        assertThatThrownBy(() -> anchorUrlBll.addOrUpdateAnchor(bo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assert code == StatusCode.SCRIPT_MONITOR_ACCOUNT_TYPE_LOCKED.getCode()
                            : "期望错误码 70007，实际: " + code;
                });
    }

    @Test
    @DisplayName("AC-001b: accountType 从 0 改为非 0，且巡检开关=1 时，抛 BusinessException(70007)")
    void addOrUpdateAnchor_accountSwitchProtection_patrolAlreadyOn_throws70007() {
        // given：DB 中 accountType=0 + 巡检=1
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 0, 1);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setAccountType(1);

        assertThatThrownBy(() -> anchorUrlBll.addOrUpdateAnchor(bo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assert code == StatusCode.SCRIPT_MONITOR_ACCOUNT_TYPE_LOCKED.getCode();
                });
    }

    // -------- Step 2：还原度解禁（Slice A 2026-06-11） --------

    /**
     * B3 解禁后：构造已确认标准稿（用于 happy path）。
     */
    private StandardScriptEntity confirmedScript() {
        StandardScriptEntity e = new StandardScriptEntity();
        e.setId(888L);
        e.setTenantId(100L);
        e.setUserId(1L);
        e.setSecUid("test-sec-uid");
        e.setIsDeleted(0);
        return e;
    }

    @Test
    @DisplayName("B3-AC-1: 还原度 0→1 + accountType=0 + 已有标准稿 → happy path 通过 + 回填 standardScriptId")
    void addOrUpdateAnchor_fidelity0To1_hasStandardScript_passAndBackfill() {
        // given：DB accountType=0 + 还原度=0 + 标准稿存在
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(standardScriptService.findValid(100L, 1L, "test-sec-uid")).thenReturn(confirmedScript());
        when(basicSettingsProducer.updateAiPartialNew(any())).thenReturn(null);
        // Step 5：还原度 0→1 实际变化 → updateByPropertyNumRetBoolean 须返 true
        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(1L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptFidelityMonitor(1); // 0→1，触发 B3 解禁路径

        // when：不抛异常
        anchorUrlBll.addOrUpdateAnchor(bo);

        // then：standardScriptId 已回填到 bo
        assert bo.getStandardScriptId() != null
                : "期望 standardScriptId 已回填，实际为 null";
        assert bo.getStandardScriptId().equals(888L)
                : "期望 standardScriptId=888，实际: " + bo.getStandardScriptId();
        verify(standardScriptService, times(1)).findValid(100L, 1L, "test-sec-uid");
        // 还原度的 commodityTypeCode 是 scriptFidelityNum，资产应被回写
        verify(userPropertyFeign, times(1))
                .updateByPropertyNumRetBoolean(1L, "scriptFidelityNum", 1L);
    }

    @Test
    @DisplayName("B3-AC-2: 还原度 0→1 + accountType=0 + 无标准稿 → 抛 70005（请先确认标准稿）")
    void addOrUpdateAnchor_fidelity0To1_noStandardScript_throws70005() {
        // given：DB accountType=0 + 还原度=0 + 标准稿不存在
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(standardScriptService.findValid(100L, 1L, "test-sec-uid")).thenReturn(null);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptFidelityMonitor(1);

        // when + then
        assertThatThrownBy(() -> anchorUrlBll.addOrUpdateAnchor(bo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assert code == StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED.getCode()
                            : "期望错误码 70005，实际: " + code;
                });
    }

    @Test
    @DisplayName("B3-AC-3: 还原度 0→1 + accountType=1（竞品）→ 抛 70004（拒绝），不查标准稿")
    void addOrUpdateAnchor_fidelity0To1_competitorAccount_throws70004() {
        // given：DB accountType=1（竞品）+ 还原度=0
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(1, 0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptFidelityMonitor(1);

        // when + then
        assertThatThrownBy(() -> anchorUrlBll.addOrUpdateAnchor(bo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assert code == StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getCode()
                            : "期望错误码 70004，实际: " + code;
                });
        // accountType=1 直接拒绝 → 不应调标准稿查询
        verify(standardScriptService, never()).findValid(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("B3-AC-4: 还原度 1→1 幂等切换 → 不触发 B3 校验路径（不查标准稿）")
    void addOrUpdateAnchor_fidelity1To1_skipsStandardScriptLookup() {
        // given：DB 中还原度已=1；请求仍设为 1（幂等）
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 1, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(basicSettingsProducer.updateAiPartialNew(any())).thenReturn(null);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptFidelityMonitor(1); // 1→1

        // when：不抛异常
        anchorUrlBll.addOrUpdateAnchor(bo);

        // then：未触发 B3（0→1）路径 → findValid 未被调用
        verify(standardScriptService, never()).findValid(anyLong(), anyLong(), anyString());
    }

    // -------- Step 3：监控位校验 --------

    @Test
    @DisplayName("AC-003: checkMonitorPosition 返回 hasSurplus=false 时，抛 BusinessException(70002)")
    void addOrUpdateAnchor_quotaNotEnough_throws70002() {
        // given：DB 中质检=0，请求改为 1；checkMonitorPosition 返回无剩余
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(userPropertyFeign.checkMonitorPosition(anyLong(), anyString()))
                .thenReturn(authNoSurplus());

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptQualityInspection(1); // 0→1，触发监控位校验

        assertThatThrownBy(() -> anchorUrlBll.addOrUpdateAnchor(bo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assert code == StatusCode.SCRIPT_MONITOR_QUOTA_NOT_ENOUGH.getCode()
                            : "期望错误码 70002，实际: " + code;
                });
    }

    // -------- Step 4：Token 校验 --------

    @Test
    @DisplayName("AC-004: aiTokenNum 余额 < 100000 时，抛 BusinessException(70001)")
    void addOrUpdateAnchor_tokenInsufficient_throws70001() {
        // given：DB 中质检=0，请求改为 1；监控位充足；Token 不足
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(userPropertyFeign.checkMonitorPosition(anyLong(), anyString()))
                .thenReturn(authWithSurplus());
        when(userPropertyFeign.getUserProperty(anyLong())).thenReturn(insufficientTokenProps());

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptQualityInspection(1);

        assertThatThrownBy(() -> anchorUrlBll.addOrUpdateAnchor(bo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assert code == StatusCode.SCRIPT_MONITOR_TOKEN_NOT_ENOUGH.getCode()
                            : "期望错误码 70001，实际: " + code;
                });
    }

    // -------- Step 5：null 字段不覆盖 + 仅变化的开关才占用 --------

    @Test
    @DisplayName("AC-005: bo.isScriptQualityInspection=null 时不调 checkMonitorPosition 不调 updateByPropertyNumRetBoolean")
    void addOrUpdateAnchor_nullFieldsDoNotOverwriteExisting() {
        // given：DB 中质检=1；请求中 isScriptQualityInspection=null（不变更）
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 1, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        // 后续写操作：updateById 成功
        when(basicSettingsProducer.updateAiPartialNew(any())).thenReturn(null);

        AddOrUpdateAnchorBo bo = baseBo();
        // 不设置 isScriptQualityInspection（null）

        // when：不应抛异常，正常执行完毕
        anchorUrlBll.addOrUpdateAnchor(bo);

        // then：不调 checkMonitorPosition 也不调 updateByPropertyNumRetBoolean
        verify(userPropertyFeign, never()).checkMonitorPosition(anyLong(), anyString());
        verify(userPropertyFeign, never()).updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("AC-006: 只传质检 0→1 时，只对 scriptQualityNum 调 countOpenSwitch + updateByPropertyNumRetBoolean，巡检不调")
    void addOrUpdateAnchor_onlyChangedSwitchOccupies() {
        // given：DB 中质检=0，巡检=0；请求只改质检 0→1
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(userPropertyFeign.checkMonitorPosition(anyLong(), anyString()))
                .thenReturn(authWithSurplus());
        when(userPropertyFeign.getUserProperty(anyLong())).thenReturn(sufficientTokenProps());
        // countOpenSwitch 返回当前开启数（包含本次切换后的值，此处模拟为 1）
        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(1L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);
        when(basicSettingsProducer.updateAiPartialNew(any())).thenReturn(null);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptQualityInspection(1); // 只开质检

        // when
        anchorUrlBll.addOrUpdateAnchor(bo);

        // then：countOpenSwitch + updateByPropertyNumRetBoolean 各调用 1 次（质检），巡检不调
        verify(anchorUrlUserProducer, times(1)).countOpenSwitch(anyLong(), anyLong(), anyString());
        verify(userPropertyFeign, times(1)).updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("AC-007: updateByPropertyNumRetBoolean 返回 false 时抛 RuntimeException（事务回滚信号）")
    void addOrUpdateAnchor_feignReturnsFalse_throwsRuntimeException_triggersRollback() {
        // given：DB 中质检=0；请求改为 1；监控位充足；Token 充足；但更新资产失败
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(userPropertyFeign.checkMonitorPosition(anyLong(), anyString()))
                .thenReturn(authWithSurplus());
        when(userPropertyFeign.getUserProperty(anyLong())).thenReturn(sufficientTokenProps());
        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(1L);
        // 模拟资产更新失败
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(false);
        when(basicSettingsProducer.updateAiPartialNew(any())).thenReturn(null);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptQualityInspection(1);

        // when + then：抛 RuntimeException（@Transactional 负责回滚，单测只验抛出）
        assertThatThrownBy(() -> anchorUrlBll.addOrUpdateAnchor(bo))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("AC-008: 同时开质检+巡检（0→1），分别对两个 code 各调 countOpenSwitch + updateByPropertyNumRetBoolean 1 次")
    void addOrUpdateAnchor_threeCapabilitiesIndependentCount() {
        // given：DB 中质检=0，巡检=0；请求同时开质检 + 巡检
        mockExistingAnchorAndUserAnchor();
        AnchorUrlUserEntity current = currentEntity(0, 0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        // 两个开关都需要 checkMonitorPosition，各返回有剩余
        when(userPropertyFeign.checkMonitorPosition(anyLong(), anyString()))
                .thenReturn(authWithSurplus());
        when(userPropertyFeign.getUserProperty(anyLong())).thenReturn(sufficientTokenProps());
        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(1L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);
        when(basicSettingsProducer.updateAiPartialNew(any())).thenReturn(null);

        AddOrUpdateAnchorBo bo = baseBo();
        bo.setIsScriptQualityInspection(1); // 质检 0→1
        bo.setIsInteractionPatrol(1);       // 巡检 0→1

        // when
        anchorUrlBll.addOrUpdateAnchor(bo);

        // then：checkMonitorPosition 调 2 次（质检 + 巡检各 1）
        // countOpenSwitch + updateByPropertyNumRetBoolean 各调 2 次
        verify(userPropertyFeign, times(2)).checkMonitorPosition(anyLong(), anyString());
        verify(anchorUrlUserProducer, times(2)).countOpenSwitch(anyLong(), anyLong(), anyString());
        verify(userPropertyFeign, times(2)).updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong());
    }
}
