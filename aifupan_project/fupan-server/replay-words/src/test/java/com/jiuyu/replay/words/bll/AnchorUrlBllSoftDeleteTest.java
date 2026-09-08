package com.jiuyu.replay.words.bll;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.words.bo.AnchorUrlUserBo;
import com.jiuyu.replay.words.entity.AnchorUrlUserEntity;
import com.jiuyu.replay.words.producer.AnchorUrlUserProducer;
import com.jiuyu.replay.words.repository.service.AnchorUrlUserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnchorUrlBll 软删配额释放路径单测（updateUserAnchor + thoroughlyDeleteAnchor）
 *
 * <p>测试范围：软删（isRemoveRecord=1）和彻底删（isRemoveRecord=2）后，三个监控开关占用的
 * 资源位（scriptQualityNum / scriptFidelityNum / interactionPatrolNum）正确释放；
 * 全部用 Mockito 纯单测，不启动 Spring 容器，不调用真实 DB / Feign。</p>
 *
 * @author beta
 * @date 2026-06-15
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnchorUrlBllSoftDeleteTest {

    // -------- 依赖 Mock --------

    @Mock
    private UserPropertyFeign userPropertyFeign;
    @Mock
    private AnchorUrlUserProducer anchorUrlUserProducer;
    @Mock
    private AnchorUrlUserService anchorUrlUserService;
    @Mock
    private UserFeign userFeign;

    /** 被测对象：手动构造 + ReflectionTestUtils 注入（mirror AnchorUrlBllUpdateMonitorSwitchTest 风格）。 */
    private AnchorUrlBll anchorUrlBll;

    /**
     * 预热 MyBatis-Plus Lambda 缓存，使 LambdaQueryWrapper 在无 Spring 容器的纯单测环境中
     * 可正常解析实体字段映射（thoroughlyDeleteAnchor 内部使用 LambdaQueryWrapper）。
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
        ReflectionTestUtils.setField(anchorUrlBll, "userFeign", userFeign);
    }

    // -------- helpers --------

    /**
     * 构造 AnchorUrlUserBo 入参（软删场景）。
     *
     * @param isRemoveRecord 软删标记（1=软删, 其他=非软删）
     * @return 构造好的 Bo
     */
    private AnchorUrlUserBo bo(Integer isRemoveRecord) {
        AnchorUrlUserBo b = new AnchorUrlUserBo();
        b.setUserId(100L);
        b.setTenantId(200L);
        b.setAnchorUrlSecUid("secUid-1");
        b.setIsRemoveRecord(isRemoveRecord);
        return b;
    }

    /**
     * 构造已存在的主播用户关联 Vo（模拟 DB 查询结果不为 null）。
     *
     * @return id=1001 的 Vo
     */
    private AnchorUrlUserVo existingVo() {
        AnchorUrlUserVo v = new AnchorUrlUserVo();
        v.setId(1001L);
        return v;
    }

    /**
     * 构造 thoroughlyDeleteAnchor 所需的用户缓存 Vo。
     *
     * @return userId=100, activeTenantId=200 的 UserCacheVo
     */
    private UserCacheVo userCache() {
        UserCacheVo uc = new UserCacheVo();
        uc.setId(100L);
        uc.setActiveTenantId(200L);
        return uc;
    }

    // -------- AC-1：质检单开，配额递减 --------

    @Test
    @DisplayName("AC-1: 软删时（isRemoveRecord=1）仅质检开关开启，3 次 Feign 均调（防御性总调），updateById 先于 countOpenSwitch")
    void updateUserAnchor_软删释放质检_配额递减1() {
        // given
        when(anchorUrlUserProducer.getUserAnchorBySecUid("secUid-1", 100L, 200L))
                .thenReturn(existingVo());
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isScriptQualityInspection"))
                .thenReturn(0L);
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isScriptFidelityMonitor"))
                .thenReturn(0L);
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isInteractionPatrol"))
                .thenReturn(0L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);

        // when
        R<String> result = anchorUrlBll.updateUserAnchor(bo(1));

        // then：先写后查（updateById 先于 countOpenSwitch）
        assertThat(result.getCode()).isEqualTo(0);
        InOrder inOrder = inOrder(anchorUrlUserProducer, userPropertyFeign);
        inOrder.verify(anchorUrlUserProducer).updateById(any());
        inOrder.verify(anchorUrlUserProducer).countOpenSwitch(100L, 200L, "isScriptQualityInspection");
        inOrder.verify(userPropertyFeign).updateByPropertyNumRetBoolean(100L, "scriptQualityNum", 0L);
        // 防御性总调：3 次 Feign 全部被调
        verify(userPropertyFeign, times(3))
                .updateByPropertyNumRetBoolean(eq(100L), anyString(), anyLong());
        // countOpenSwitch 被调 3 次（三个 switch 字段各一次）
        verify(anchorUrlUserProducer, times(3))
                .countOpenSwitch(eq(100L), eq(200L), anyString());
    }

    // -------- AC-4：三开关全开，三配额各递减 --------

    @Test
    @DisplayName("AC-4: 三个开关均为 1 时，三个 code 的 Feign 各传正确 openCount")
    void updateUserAnchor_三开关全开_三配额各递减1() {
        // given：模拟软删后其他主播中仍开启各开关的数量
        when(anchorUrlUserProducer.getUserAnchorBySecUid("secUid-1", 100L, 200L))
                .thenReturn(existingVo());
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isScriptQualityInspection"))
                .thenReturn(5L);
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isScriptFidelityMonitor"))
                .thenReturn(3L);
        when(anchorUrlUserProducer.countOpenSwitch(100L, 200L, "isInteractionPatrol"))
                .thenReturn(7L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);

        // when
        R<String> result = anchorUrlBll.updateUserAnchor(bo(1));

        // then：三个 Feign 各传对应 openCount
        assertThat(result.getCode()).isEqualTo(0);
        verify(userPropertyFeign).updateByPropertyNumRetBoolean(100L, "scriptQualityNum", 5L);
        verify(userPropertyFeign).updateByPropertyNumRetBoolean(100L, "scriptFidelityNum", 3L);
        verify(userPropertyFeign).updateByPropertyNumRetBoolean(100L, "interactionPatrolNum", 7L);
    }

    // -------- AC-5：thoroughlyDeleteAnchor 释放配额 --------

    @Test
    @DisplayName("AC-5: thoroughlyDeleteAnchor 触发释放，updateBatchById 在前、Feign 在后")
    void thoroughlyDeleteAnchor_释放配额() {
        // given
        when(userFeign.getLocalUser()).thenReturn(R.ok(userCache()));

        AnchorUrlUserEntity entity = new AnchorUrlUserEntity();
        entity.setId(1001L);
        entity.setIsRemoveRecord(0);
        when(anchorUrlUserService.list(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(entity));
        when(anchorUrlUserService.updateBatchById(anyList())).thenReturn(true);

        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(0L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);

        // when
        boolean result = anchorUrlBll.thoroughlyDeleteAnchor("secUid-1");

        // then
        assertThat(result).isTrue();
        // entity 已被 setIsRemoveRecord(2)
        assertThat(entity.getIsRemoveRecord()).isEqualTo(2);
        // updateBatchById 在 Feign 之前
        InOrder inOrder = inOrder(anchorUrlUserService, anchorUrlUserProducer, userPropertyFeign);
        inOrder.verify(anchorUrlUserService).updateBatchById(anyList());
        inOrder.verify(anchorUrlUserProducer).countOpenSwitch(100L, 200L, "isScriptQualityInspection");
        inOrder.verify(userPropertyFeign).updateByPropertyNumRetBoolean(100L, "scriptQualityNum", 0L);
        // 三个开关均触发：3 次 countOpenSwitch + 3 次 Feign（防御性总调）
        verify(anchorUrlUserProducer, times(3))
                .countOpenSwitch(eq(100L), eq(200L), anyString());
        verify(userPropertyFeign, times(3))
                .updateByPropertyNumRetBoolean(eq(100L), anyString(), eq(0L));
    }

    // -------- AC-6：全 0 防御性总调 --------

    @Test
    @DisplayName("AC-6: 三开关全为 0 时，3 次 Feign 仍调（防御性总调，openCount=0 传 Feign）")
    void updateUserAnchor_全0防御性调Feign_配额不变() {
        // given
        when(anchorUrlUserProducer.getUserAnchorBySecUid("secUid-1", 100L, 200L))
                .thenReturn(existingVo());
        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(0L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);

        // when
        R<String> result = anchorUrlBll.updateUserAnchor(bo(1));

        // then：即使 openCount=0，仍调 3 次 Feign（不做 openCount=0 跳过短路）
        assertThat(result.getCode()).isEqualTo(0);
        verify(userPropertyFeign, times(3))
                .updateByPropertyNumRetBoolean(eq(100L), anyString(), eq(0L));
    }

    // -------- AC-7：Feign 返 false → RuntimeException fail-fast --------

    @Test
    @DisplayName("AC-7: 第一个 Feign 返 false 时抛 RuntimeException，后续 Feign 不再调用（fail-fast 短路）")
    void updateUserAnchor_Feign返false_抛RuntimeException() {
        // given
        when(anchorUrlUserProducer.getUserAnchorBySecUid("secUid-1", 100L, 200L))
                .thenReturn(existingVo());
        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(1L);
        // 第一个 code（scriptQualityNum）就返回 false → fail-fast
        when(userPropertyFeign.updateByPropertyNumRetBoolean(100L, "scriptQualityNum", 1L))
                .thenReturn(false);

        // when + then：异常消息双重锁定首个失败 code，确认是 fail-fast 在第一个 code 触发
        assertThatThrownBy(() -> anchorUrlBll.updateUserAnchor(bo(1)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("资产占用量更新失败")
                .hasMessageContaining("code=scriptQualityNum");

        // fail-fast：第一个 code false 后，后两个 code 不再调用
        verify(userPropertyFeign, times(1))
                .updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong());
        // updateById 仍发生（回滚由 @Transactional 处理，单测不验回滚 SQL）
        verify(anchorUrlUserProducer).updateById(any());
    }

    // -------- AC-9：重复软删幂等 --------

    @Test
    @DisplayName("AC-9: 重复软删同一主播（再调 isRemoveRecord=1），Feign 仍调 3 次但 openCount 与上次一致，幂等无异常")
    void updateUserAnchor_重复软删幂等() {
        // given：第二次软删时，行已是 isRemoveRecord=1，countOpenSwitch 过滤后返回稳定值
        when(anchorUrlUserProducer.getUserAnchorBySecUid("secUid-1", 100L, 200L))
                .thenReturn(existingVo());
        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(2L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);

        // when
        R<String> result = anchorUrlBll.updateUserAnchor(bo(1));

        // then：覆盖写幂等，不抛异常，Feign 仍调 3 次
        assertThat(result.getCode()).isEqualTo(0);
        verify(userPropertyFeign, times(3))
                .updateByPropertyNumRetBoolean(eq(100L), anyString(), eq(2L));
    }

    // -------- 软删置零三开关（2026-06-22 改造）--------

    @Test
    @DisplayName("软删置零: isRemoveRecord=1 时，落库 bo 的三个监控开关字段被置 0（updateById 收到的 bo）")
    void updateUserAnchor_softDelete_zerosThreeSwitchFieldsOnBo() {
        // given：bo 初始三开关均为 1（前端可能带上旧值），软删后应被强制置 0
        when(anchorUrlUserProducer.getUserAnchorBySecUid("secUid-1", 100L, 200L))
                .thenReturn(existingVo());
        when(anchorUrlUserProducer.countOpenSwitch(anyLong(), anyLong(), anyString())).thenReturn(0L);
        when(userPropertyFeign.updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong()))
                .thenReturn(true);

        AnchorUrlUserBo b = bo(1);
        b.setIsScriptQualityInspection(1);
        b.setIsScriptFidelityMonitor(1);
        b.setIsInteractionPatrol(1);

        // when
        anchorUrlBll.updateUserAnchor(b);

        // then：捕获传给 updateById 的 bo，断言三开关已被置 0
        ArgumentCaptor<AnchorUrlUserBo> boCaptor = ArgumentCaptor.forClass(AnchorUrlUserBo.class);
        verify(anchorUrlUserProducer).updateById(boCaptor.capture());
        AnchorUrlUserBo persisted = boCaptor.getValue();
        assertThat(persisted.getIsScriptQualityInspection()).isEqualTo(0);
        assertThat(persisted.getIsScriptFidelityMonitor()).isEqualTo(0);
        assertThat(persisted.getIsInteractionPatrol()).isEqualTo(0);
    }

    @Test
    @DisplayName("非软删不误伤: isRemoveRecord=0 时，bo 的三开关字段保持原值（不被置 0）")
    void updateUserAnchor_normalUpdate_doesNotZeroSwitchFields() {
        // given：普通修改（isRemoveRecord=0），bo 带三开关=1，不应被改动
        when(anchorUrlUserProducer.getUserAnchorBySecUid("secUid-1", 100L, 200L))
                .thenReturn(existingVo());

        AnchorUrlUserBo b = new AnchorUrlUserBo();
        b.setUserId(100L);
        b.setTenantId(200L);
        b.setAnchorUrlSecUid("secUid-1");
        b.setIsRemoveRecord(0);
        b.setIsScriptQualityInspection(1);
        b.setIsScriptFidelityMonitor(1);
        b.setIsInteractionPatrol(1);

        // when
        anchorUrlBll.updateUserAnchor(b);

        // then：捕获 updateById 的 bo，三开关保持原值 1（未被软删分支置 0）
        ArgumentCaptor<AnchorUrlUserBo> boCaptor = ArgumentCaptor.forClass(AnchorUrlUserBo.class);
        verify(anchorUrlUserProducer).updateById(boCaptor.capture());
        AnchorUrlUserBo persisted = boCaptor.getValue();
        assertThat(persisted.getIsScriptQualityInspection()).isEqualTo(1);
        assertThat(persisted.getIsScriptFidelityMonitor()).isEqualTo(1);
        assertThat(persisted.getIsInteractionPatrol()).isEqualTo(1);
    }

    // -------- Gap-4：isRemoveRecord 非 1 时不触发释放循环 --------

    @Test
    @DisplayName("AC-Gap4: isRemoveRecord=0（非软删）时，countOpenSwitch 和 Feign 均不调用")
    void updateUserAnchor_isRemoveRecord为0_不触发释放循环() {
        // given：vo 存在，但 bo.isRemoveRecord=0（非软删更新）
        when(anchorUrlUserProducer.getUserAnchorBySecUid("secUid-1", 100L, 200L))
                .thenReturn(existingVo());

        // when：isRemoveRecord=0
        AnchorUrlUserBo b = new AnchorUrlUserBo();
        b.setUserId(100L);
        b.setTenantId(200L);
        b.setAnchorUrlSecUid("secUid-1");
        b.setIsRemoveRecord(0);
        anchorUrlBll.updateUserAnchor(b);

        // then：不触发释放循环
        verify(anchorUrlUserProducer, never()).countOpenSwitch(anyLong(), anyLong(), anyString());
        verify(userPropertyFeign, never()).updateByPropertyNumRetBoolean(anyLong(), anyString(), anyLong());
    }
}
