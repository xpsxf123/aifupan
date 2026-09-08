package com.jiuyu.replay.words.bll;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.MonitorPositionAuthVo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.entity.StandardScriptEntity;
import com.jiuyu.replay.words.producer.AnchorUrlUserProducer;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import com.jiuyu.replay.words.repository.service.StandardScriptService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnchorUrlBll#updateMonitorSwitch 单测（B8 监控位开关切换逻辑）
 *
 * <p>测试范围：updateMonitorSwitch 全路径（enabled 校验 / 主播不存在 / 幂等短路 /
 * 还原度暂禁 / 监控位不足 / 正常开启写库 / 正常关闭释放），
 * 全部用 Mockito 纯单测，不启动 Spring 容器，不调用真实 DB / Feign。</p>
 *
 * @author beta
 * @date 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnchorUrlBllUpdateMonitorSwitchTest {

    // -------- 依赖 Mock --------

    @Mock
    private AnchorUrlUserService anchorUrlUserService;
    @Mock
    private UserPropertyFeign userPropertyFeign;
    @Mock
    private AnchorUrlUserProducer anchorUrlUserProducer;
    @Mock
    private StandardScriptService standardScriptService;

    /** 被测对象：手动构造 + ReflectionTestUtils 注入（mirror B6 风格）。 */
    private AnchorUrlBll anchorUrlBll;

    /**
     * 预热 MyBatis-Plus Lambda 缓存，使 LambdaUpdateWrapper#set(SFunction, V) 在无 Spring
     * 容器的纯单测环境中可正常解析实体字段映射。
     */
    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new Configuration(), ""),
                AnchorUrlUserEntity.class);
    }

    @BeforeEach
    void setUp() {
        anchorUrlBll = new AnchorUrlBll();
        ReflectionTestUtils.setField(anchorUrlBll, "anchorUrlUserService", anchorUrlUserService);
        ReflectionTestUtils.setField(anchorUrlBll, "userPropertyFeign", userPropertyFeign);
        ReflectionTestUtils.setField(anchorUrlBll, "anchorUrlUserProducer", anchorUrlUserProducer);
        ReflectionTestUtils.setField(anchorUrlBll, "standardScriptService", standardScriptService);
    }

    // -------- helpers --------

    /**
     * 构造主播-用户关联实体（DB 查询结果）。
     *
     * @param isScriptQualityInspection 话术质检开关当前值
     * @param isScriptFidelityMonitor   话术还原度开关当前值
     * @param isInteractionPatrol       互动巡检开关当前值
     * @return 构造好的实体
     */
    private AnchorUrlUserEntity currentEntity(Integer isScriptQualityInspection,
                                               Integer isScriptFidelityMonitor,
                                               Integer isInteractionPatrol) {
        AnchorUrlUserEntity entity = new AnchorUrlUserEntity();
        entity.setId(1001L);
        entity.setAnchorUrlSecUid("secUid-1");
        entity.setUserId(100L);
        entity.setTenantId(200L);
        entity.setIsScriptQualityInspection(isScriptQualityInspection);
        entity.setIsScriptFidelityMonitor(isScriptFidelityMonitor);
        entity.setIsInteractionPatrol(isInteractionPatrol);
        return entity;
    }

    /**
     * 构造监控位充足的授权 Vo。
     *
     * @return hasSurplus=true 的 Vo
     */
    private MonitorPositionAuthVo authWithSurplus() {
        MonitorPositionAuthVo vo = new MonitorPositionAuthVo();
        vo.setHasSurplus(true);
        return vo;
    }

    /**
     * 构造监控位不足的授权 Vo。
     *
     * @return hasSurplus=false 的 Vo
     */
    private MonitorPositionAuthVo authNoSurplus() {
        MonitorPositionAuthVo vo = new MonitorPositionAuthVo();
        vo.setHasSurplus(false);
        return vo;
    }

    // -------- AC-001：质检 0→1 happy path --------

    @Test
    @DisplayName("AC-001: 质检开关 0→1，监控位充足，验证写库 + countOpenSwitch + updateByPropertyNumRetBoolean + 返回 R.ok")
    void updateMonitorSwitch_quality0To1_happy() {
        // given：DB 中质检=0；监控位充足；资产更新成功
        AnchorUrlUserEntity current = currentEntity(0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(userPropertyFeign.checkMonitorPosition(100L, "scriptQualityNum"))
                .thenReturn(authWithSurplus());
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isScriptQualityInspection"))
                .thenReturn(5L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(100L, "scriptQualityNum", 5L))
                .thenReturn(true);

        // when：monitorType=0(质检) enabled=1
        R<Boolean> result = anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 0, 1);

        // then
        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).isTrue();
        verify(anchorUrlUserService).update(any(LambdaUpdateWrapper.class));
        verify(anchorUrlUserProducer).countOpenSwitch(100L, 200L, "isScriptQualityInspection");
        verify(userPropertyFeign).updateByPropertyNumRetBoolean(100L, "scriptQualityNum", 5L);
    }

    // -------- AC-002：巡检 1→0，释放监控位 --------

    @Test
    @DisplayName("AC-002: 巡检开关 1→0，验证写库 + countOpenSwitch + updateByPropertyNumRetBoolean；不调 checkMonitorPosition")
    void updateMonitorSwitch_patrol1To0_releases() {
        // given：DB 中巡检=1；资产更新成功
        AnchorUrlUserEntity current = currentEntity(0, 0, 1);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isInteractionPatrol"))
                .thenReturn(2L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(100L, "interactionPatrolNum", 2L))
                .thenReturn(true);

        // when：monitorType=2(巡检) enabled=0
        R<Boolean> result = anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 2, 0);

        // then：关闭路径不校验监控位
        assertThat(result.getCode()).isEqualTo(0);
        verify(userPropertyFeign, never()).checkMonitorPosition(anyLong(), anyString());
        verify(anchorUrlUserService).update(any(LambdaUpdateWrapper.class));
        verify(anchorUrlUserProducer).countOpenSwitch(100L, 200L, "isInteractionPatrol");
        verify(userPropertyFeign).updateByPropertyNumRetBoolean(100L, "interactionPatrolNum", 2L);
    }

    // -------- B8 解禁（Slice A 2026-06-11）：还原度 0→1 新增校验逻辑 --------

    /**
     * 构造带 accountType 的实体（B8 解禁分支需要 accountType 校验）。
     *
     * @param accountType              账号归属类型
     * @param isScriptFidelityMonitor  还原度开关当前值
     * @return 构造好的实体
     */
    private AnchorUrlUserEntity entityWithAccountType(Integer accountType, Integer isScriptFidelityMonitor) {
        AnchorUrlUserEntity entity = currentEntity(0, isScriptFidelityMonitor, 0);
        entity.setAccountType(accountType);
        return entity;
    }

    /**
     * 构造已确认标准稿（B8 解禁 happy path 需要）。
     */
    private StandardScriptEntity confirmedScript() {
        StandardScriptEntity e = new StandardScriptEntity();
        e.setId(777L);
        e.setTenantId(200L);
        e.setUserId(100L);
        e.setSecUid("secUid-1");
        e.setIsDeleted(0);
        return e;
    }

    @Test
    @DisplayName("B8-AC-1: 还原度 0→1 + accountType=0 + 已有标准稿 → happy path 通过 + 写库 + 回填 standardScriptId")
    void updateMonitorSwitch_fidelity0To1_hasStandardScript_pass() {
        // given：accountType=0 自有账号 + 还原度=0 + 标准稿存在 + 监控位充足
        AnchorUrlUserEntity current = entityWithAccountType(0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(standardScriptService.findValid(200L, 100L, "secUid-1")).thenReturn(confirmedScript());
        when(userPropertyFeign.checkMonitorPosition(100L, "scriptFidelityNum"))
                .thenReturn(authWithSurplus());
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isScriptFidelityMonitor"))
                .thenReturn(1L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(100L, "scriptFidelityNum", 1L))
                .thenReturn(true);

        // when：monitorType=1(还原度) enabled=1
        R<Boolean> result = anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 1, 1);

        // then
        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).isTrue();
        verify(standardScriptService, times(1)).findValid(200L, 100L, "secUid-1");
        verify(anchorUrlUserService).update(any(LambdaUpdateWrapper.class));
        verify(anchorUrlUserProducer).countOpenSwitch(100L, 200L, "isScriptFidelityMonitor");
        verify(userPropertyFeign).updateByPropertyNumRetBoolean(100L, "scriptFidelityNum", 1L);
    }

    @Test
    @DisplayName("B8-AC-2: 还原度 0→1 + accountType=0 + 无标准稿 → 抛 70005（请先确认标准稿）")
    void updateMonitorSwitch_fidelity0To1_noStandardScript_throws70005() {
        // given：accountType=0 + 还原度=0 + 标准稿不存在
        AnchorUrlUserEntity current = entityWithAccountType(0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(standardScriptService.findValid(200L, 100L, "secUid-1")).thenReturn(null);

        // when + then
        assertThatThrownBy(() -> anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 1, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assertThat(code).isEqualTo(
                            StatusCode.SCRIPT_MONITOR_STANDARD_SCRIPT_UNCONFIRMED.getCode());
                });
        // 业务校验失败，不应执行写库
        verify(anchorUrlUserService, never()).update(any(LambdaUpdateWrapper.class));
        // 也不应该校验监控位
        verify(userPropertyFeign, never()).checkMonitorPosition(anyLong(), anyString());
    }

    @Test
    @DisplayName("B8-AC-3: 还原度 0→1 + accountType=1（竞品）→ 抛 70004，不查标准稿")
    void updateMonitorSwitch_fidelity0To1_competitorAccount_throws70004() {
        // given：accountType=1（竞品）+ 还原度=0
        AnchorUrlUserEntity current = entityWithAccountType(1, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);

        // when + then
        assertThatThrownBy(() -> anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 1, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assertThat(code).isEqualTo(StatusCode.SCRIPT_MONITOR_NOT_OWN_ACCOUNT.getCode());
                });
        // accountType=1 直接拒绝 → 不查标准稿
        verify(standardScriptService, never()).findValid(anyLong(), anyLong(), anyString());
        // 也不应写库
        verify(anchorUrlUserService, never()).update(any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("B8-AC-4: 还原度 1→0（关闭路径）→ 不触发 B8 校验（不查标准稿），照常关闭释放")
    void updateMonitorSwitch_fidelity1To0_doesNotTriggerB8Validation() {
        // given：还原度当前=1，请求 enabled=0（关闭）；释放路径不校验监控位
        AnchorUrlUserEntity current = entityWithAccountType(0, 1);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isScriptFidelityMonitor"))
                .thenReturn(0L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(100L, "scriptFidelityNum", 0L))
                .thenReturn(true);

        // when：monitorType=1 enabled=0
        R<Boolean> result = anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 1, 0);

        // then
        assertThat(result.getCode()).isEqualTo(0);
        verify(standardScriptService, never()).findValid(anyLong(), anyLong(), anyString());
        verify(userPropertyFeign, never()).checkMonitorPosition(anyLong(), anyString());
        verify(anchorUrlUserService).update(any(LambdaUpdateWrapper.class));
    }

    // -------- AC-004：监控位不足抛 70002 --------

    @Test
    @DisplayName("AC-004: 质检 0→1，checkMonitorPosition 返回 hasSurplus=false，抛 BusinessException(70002)")
    void updateMonitorSwitch_quotaNotEnough_throws70002() {
        // given：DB 中质检=0；checkMonitorPosition 返回无剩余
        AnchorUrlUserEntity current = currentEntity(0, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        when(userPropertyFeign.checkMonitorPosition(anyLong(), anyString()))
                .thenReturn(authNoSurplus());

        // when + then
        assertThatThrownBy(() -> anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 0, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assertThat(code).isEqualTo(StatusCode.SCRIPT_MONITOR_QUOTA_NOT_ENOUGH.getCode());
                });
    }

    // -------- AC-005：状态无变化短路返回 ok，不调 Feign --------

    @Test
    @DisplayName("AC-005: 质检当前=1，enabled=1，幂等短路直接返回 R.ok；不调 checkMonitorPosition / 写库 / countOpenSwitch")
    void updateMonitorSwitch_noStatusChange_shortCircuitsReturnOkSkipsFeign() {
        // given：DB 中质检已=1；请求仍设 enabled=1
        AnchorUrlUserEntity current = currentEntity(1, 0, 0);
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(current);

        // when
        R<Boolean> result = anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 0, 1);

        // then：直接短路，所有写操作不调用
        assertThat(result.getCode()).isEqualTo(0);
        assertThat(result.getData()).isTrue();
        verify(userPropertyFeign, never()).checkMonitorPosition(anyLong(), anyString());
        verify(anchorUrlUserService, never()).update(any(LambdaUpdateWrapper.class));
        verify(anchorUrlUserProducer, never()).countOpenSwitch(anyLong(), anyLong(), anyString());
        verify(userPropertyFeign, never()).updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong());
    }

    // -------- AC-006：anchor 不存在抛 30000 --------

    @Test
    @DisplayName("AC-006: anchorUrlUserService.getOne 返回 null，抛 BusinessException(30000 DATA_NOT_EXIST)")
    void updateMonitorSwitch_anchorNotExist_throws30000() {
        // given：getOne 返回 null
        when(anchorUrlUserService.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // when + then
        assertThatThrownBy(() -> anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-not-exist", 0, 1))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assertThat(code).isEqualTo(StatusCode.DATA_NOT_EXIST.getCode());
                });
    }

    // -------- AC-007：enabled 非法值抛 70013 --------

    @Test
    @DisplayName("AC-007: enabled=99 非法值，抛 BusinessException(70013 SCRIPT_MONITOR_PARAM_INVALID)")
    void updateMonitorSwitch_enabledInvalid_throws70013() {
        // when + then：enabled=99 在查 DB 之前就应被拦截
        assertThatThrownBy(() -> anchorUrlBll.updateMonitorSwitch(100L, 200L, "secUid-1", 0, 99))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    int code = ((BusinessException) ex).getCode();
                    assertThat(code).isEqualTo(StatusCode.SCRIPT_MONITOR_PARAM_INVALID.getCode());
                });

        // getOne 根本不应被调用
        verify(anchorUrlUserService, never()).getOne(any(LambdaQueryWrapper.class));
    }
}
