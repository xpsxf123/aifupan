package com.jiuyu.replay.ai.bll;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.jiuyu.replay.ai.bo.TriggerReportBo;
import com.jiuyu.replay.ai.config.ScriptMonitorMqProperties;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.generic.enums.words.MonitorTypeEnum;
import com.jiuyu.replay.generic.enums.words.ScriptMonitorStatusEnum;
import com.jiuyu.replay.generic.feign.order.AiTokenWithholdFeign;
import com.jiuyu.replay.generic.feign.order.UserPropertyFeign;
import com.jiuyu.replay.generic.feign.power.UserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.feign.words.AnchorVideoFeign;
import com.jiuyu.replay.generic.feign.words.BasicSettingsFeign;
import com.jiuyu.replay.generic.feign.words.SensitiveWordsFeign;
import com.jiuyu.replay.generic.feign.words.StandardScriptFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.common.code.StatusCode;
import com.jiuyu.replay.generic.vo.common.exception.BusinessException;
import com.jiuyu.replay.generic.vo.order.UserPropertyTypeInfoVo;
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorBll 自动触发单测（B7）
 *
 * <p>覆盖 autoTriggerReport 的 happy path / 已有 GENERATED 重置 / 并发 BusinessException 降级
 * 以及 triggerSource 字段在手动/自动场景下的正确赋值。</p>
 *
 * @author beta
 * @date 2026-06-02
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorBllAutoTriggerTest {

    @Mock
    private ScriptMonitorReportService reportService;
    @Mock
    private ScriptMonitorReportWriteService writeService;
    @Mock
    private ScriptMonitorReportBodyRepository reportBodyRepository;
    @Mock
    private AnchorUrlUserFeign anchorUrlUserFeign;
    @Mock
    private AnchorVideoFeign anchorVideoFeign;
    @Mock
    private SensitiveWordsFeign sensitiveWordsFeign;
    @Mock
    private UserFeign userFeign;
    @Mock
    private UserPropertyFeign userPropertyFeign;
    @Mock
    private AiTokenWithholdFeign aiTokenWithholdFeign;
    @Mock
    private RocketMqBll rocketMqBll;
    @Mock
    private ScriptMonitorMqProperties scriptMonitorMqProperties;
    @Mock
    private StandardScriptFeign standardScriptFeign;
    @Mock
    private BasicSettingsFeign basicSettingsFeign;

    private ScriptMonitorBll bll;

    @BeforeEach
    void setUp() {
        // 手动构造（构造器注入，B9 移除了 readService + roleConfirmService 两个参数）
        bll = new ScriptMonitorBll(
                reportService,
                writeService,
                reportBodyRepository,
                anchorUrlUserFeign,
                anchorVideoFeign,
                sensitiveWordsFeign,
                userFeign,
                userPropertyFeign,
                aiTokenWithholdFeign,
                rocketMqBll,
                scriptMonitorMqProperties,
                standardScriptFeign,
                basicSettingsFeign
        );
        // 默认 MQ Properties mock
        ScriptMonitorMqProperties.BusinessTags tags = new ScriptMonitorMqProperties.BusinessTags();
        tags.setTagQualityInspection("Tag_Quality_Inspection");
        tags.setTagInteractionPatrol("Tag_Interaction_Patrol");
        when(scriptMonitorMqProperties.getBusinessTags()).thenReturn(tags);
        when(scriptMonitorMqProperties.getTopic()).thenReturn("script_monitor_test_topic");
        // 默认 MQ 直发成功
        when(rocketMqBll.syncSendAndDeliverToTopic(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        // 默认 userFeign（部分测试不需要，LENIENT 模式不报 unnecessary stubbing）
        UserCacheVo user = new UserCacheVo();
        user.setId(1L);
        user.setActiveTenantId(100L);
        when(userFeign.getLocalUser()).thenReturn(R.ok(user));
    }

    // ---------- helpers ----------

    /**
     * 构建模拟报告查询链（LambdaQueryChainWrapper）
     */
    @SuppressWarnings("unchecked")
    private LambdaQueryChainWrapper<ScriptMonitorReportEntity> mockReportQuery(ScriptMonitorReportEntity returnVal) {
        LambdaQueryChainWrapper<ScriptMonitorReportEntity> q = mock(LambdaQueryChainWrapper.class);
        doReturn(q).when(q).eq(any(), any());
        doReturn(q).when(q).in(any(), anyCollection());
        doReturn(q).when(q).last(anyString());
        when(q.one()).thenReturn(returnVal);
        when(reportService.lambdaQuery()).thenReturn(q);
        return q;
    }

    /**
     * 构建模拟报告更新链（LambdaUpdateChainWrapper）。
     *
     * <p>B9 hotfix：handleExistingAndReturn 改用 lambdaUpdate().set(...).update() 显式重置 isRead/confirmedAt，
     * 替代原 updateById(exist) partial entity update。此 helper 让链上每个 set/eq 返回 self，update() 返 true。</p>
     */
    @SuppressWarnings("unchecked")
    private LambdaUpdateChainWrapper<ScriptMonitorReportEntity> mockReportLambdaUpdate() {
        LambdaUpdateChainWrapper<ScriptMonitorReportEntity> u = mock(LambdaUpdateChainWrapper.class);
        doReturn(u).when(u).eq(any(), any());
        doReturn(u).when(u).set(any(), any());
        when(u.update()).thenReturn(true);
        when(reportService.lambdaUpdate()).thenReturn(u);
        return u;
    }

    private ScriptMonitorReportEntity report(Integer status) {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(5L);
        r.setTenantId(100L);
        r.setUserId(1L);
        r.setSourceType(0);
        // HOTFIX 2026-06-06: 录制视频 sourceType=0 对应 sceneType=0 复盘场景（原值 1 视频分析为 bug）
        r.setSceneType(0);
        r.setSourceId("v1");
        r.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setStatus(status);
        r.setTriggerSource("auto");
        r.setIsDeleted(0);
        r.setCreateDate(new Date());
        r.setUpdateDate(new Date());
        return r;
    }

    private List<UserPropertyTypeInfoVo> tokenProps(long total, long use) {
        UserPropertyTypeInfoVo v = new UserPropertyTypeInfoVo();
        v.setCommodityTypeCode("aiTokenNum");
        v.setTotalQuantity(total);
        v.setUseQuantity(use);
        return List.of(v);
    }

    // ---------- autoTriggerReport 测试 ----------

    @Test
    @DisplayName("autoTriggerReport 首次触发（无已有报告）happy path — triggerSource=auto")
    void autoTriggerReport_qualityHappy_buildsReport_triggerSourceAuto() {
        // given：无已有报告
        mockReportQuery(null);
        when(reportService.save(any())).thenReturn(true);

        // when
        boolean result = bll.autoTriggerReport("v1", 1L, 100L, MonitorTypeEnum.QUALITY_INSPECTION.getCode());

        // then
        assertThat(result).isTrue();
        // 验 save 被调用，且 triggerSource='auto'
        verify(reportService).save(argThat(r -> "auto".equals(r.getTriggerSource())));
        // HOTFIX 2026-06-06: 验 sceneType=0 复盘场景（原 bug 硬编码 1 视频分析导致 reportStatus 查不到）
        verify(reportService).save(argThat(r -> Integer.valueOf(0).equals(r.getSceneType())));
        // 验 MQ 直发被调用
        verify(rocketMqBll).syncSendAndDeliverToTopic(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("autoTriggerReport 已有 GENERATED 报告时走 handleExisting 重置 — triggerSource 覆写为 auto")
    void autoTriggerReport_existingGeneratedReport_handleExistingResets_triggerSourceKeepsAuto() {
        // given：已有 GENERATED 报告
        ScriptMonitorReportEntity existing = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        existing.setTriggerSource("manual"); // 旧值手动，自动触发后应被覆写为 auto
        mockReportQuery(existing);
        // B9 hotfix：handleExistingAndReturn 改用 lambdaUpdate().set(...).update() 显式重置
        LambdaUpdateChainWrapper<ScriptMonitorReportEntity> updateChain = mockReportLambdaUpdate();

        // when
        boolean result = bll.autoTriggerReport("v1", 1L, 100L, MonitorTypeEnum.QUALITY_INSPECTION.getCode());

        // then
        assertThat(result).isTrue();
        // 验 lambdaUpdate 链被调用执行（update() 被触发证明 set/eq 链路走完）
        verify(updateChain).update();
        // handleExistingAndReturn 同步设 existing 内存为 auto（DB 写入由 lambdaUpdate 完成）
        assertThat(existing.getTriggerSource()).isEqualTo("auto");
        // 验 MQ 直发被调用
        verify(rocketMqBll).syncSendAndDeliverToTopic(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    // ---------- B9 AC-013: handleExistingAndReturn 重置 isRead=0 + confirmedAt=NULL ----------

    @Test
    @DisplayName("AC-013: handleExistingAndReturn 重新生成时重置 is_read=0 + confirmed_at=NULL")
    void handleExistingAndReturn_resetsIsRead0AndConfirmedAtNull() {
        // given：已有 GENERATED 报告且 is_read=1 + confirmed_at 非 null
        ScriptMonitorReportEntity existing = report(ScriptMonitorStatusEnum.GENERATED.getCode());
        existing.setIsRead(1);
        existing.setConfirmedAt(new Date());
        existing.setTriggerSource("manual");
        mockReportQuery(existing);
        // B9 hotfix：handleExistingAndReturn 改用 lambdaUpdate().set(...).update() 实现重置
        LambdaUpdateChainWrapper<ScriptMonitorReportEntity> updateChain = mockReportLambdaUpdate();

        // when：自动触发走 handleExistingAndReturn 路径
        boolean result = bll.autoTriggerReport("v1", 1L, 100L, MonitorTypeEnum.QUALITY_INSPECTION.getCode());

        // then
        assertThat(result).isTrue();
        // AC-013 关键断言：lambdaUpdate 链执行 + handleExistingAndReturn 同步设 existing 内存
        // （DB 写入由 lambdaUpdate 完成，内存同步保证后续 generate() 读 entity 一致）
        verify(updateChain).update();
        assertThat(existing.getIsRead()).isEqualTo(0);
        assertThat(existing.getConfirmedAt()).isNull();
        assertThat(existing.getTriggerSource()).isEqualTo("auto");
    }

    @Test
    @DisplayName("autoTriggerReport 并发 GENERATING — createGeneratingTaskAndReturn 抛 BusinessException 降级为 false")
    void autoTriggerReport_concurrentGenerating_businessException_downgradedToWarnLog() {
        // given：已有 GENERATING 报告（防重 skip 场景）
        ScriptMonitorReportEntity existing = report(ScriptMonitorStatusEnum.GENERATING.getCode());
        mockReportQuery(existing);

        // when
        boolean result = bll.autoTriggerReport("v1", 1L, 100L, MonitorTypeEnum.QUALITY_INSPECTION.getCode());

        // then：GENERATING 时直接 skip 返回 false，不投 MQ
        assertThat(result).isFalse();
        verify(rocketMqBll, never()).syncSendAndDeliverToTopic(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("手动 triggerReport 调 createGeneratingTaskAndReturn 时 triggerSource=manual")
    void triggerReport_manualCall_triggerSourceManual() {
        // given
        mockReportQuery(null);
        when(reportService.save(any())).thenReturn(true);
        // Token 余额充足
        when(userPropertyFeign.getUserProperty(anyLong())).thenReturn(tokenProps(200000L, 0L));
        // userFeign 提供 user（triggerReport 走 currentUser()）
        UserCacheVo user = new UserCacheVo();
        user.setId(1L);
        user.setActiveTenantId(100L);
        when(userFeign.getLocalUser()).thenReturn(R.ok(user));
        // 资源存在且归属正确
        com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo video = new com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo();
        video.setVideoId("v1");
        video.setTenantId(100L);
        video.setUserId(1L);
        video.setExistBarrage(0);
        when(anchorVideoFeign.GetByVideoId(anyString())).thenReturn(R.ok(video));
        // 预扣 Token 成功
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong()))
                .thenReturn(new com.jiuyu.replay.generic.vo.order.RedisWithholdVo());
        // MQ 直发成功
        when(rocketMqBll.syncSendAndDeliverToTopic(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        // when
        TriggerReportBo bo = new TriggerReportBo();
        bo.setSourceType(0);
        // HOTFIX 2026-06-06: sourceType=0 录制视频对应 sceneType=0 复盘场景（合法组合）
        bo.setSceneType(0);
        bo.setSourceId("v1");
        bo.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        bll.triggerReport(bo);

        // then：新建报告时 triggerSource='manual'
        verify(reportService).save(argThat(r -> "manual".equals(r.getTriggerSource())));
    }

    @Test
    @DisplayName("handleExistingAndReturn 手动重触发 — 已有 auto 记录时覆写 triggerSource 为 manual")
    void handleExistingAndReturn_manualRetrigger_overridesTriggerSourceToManual() {
        // given：已有 NOT_GENERATED（0）报告，原 triggerSource='auto'
        ScriptMonitorReportEntity existing = report(ScriptMonitorStatusEnum.NOT_GENERATED.getCode());
        existing.setTriggerSource("auto");
        mockReportQuery(existing);
        // B9 hotfix：handleExistingAndReturn 改用 lambdaUpdate().set(...).update()
        LambdaUpdateChainWrapper<ScriptMonitorReportEntity> updateChain = mockReportLambdaUpdate();
        // Token 余额充足
        when(userPropertyFeign.getUserProperty(anyLong())).thenReturn(tokenProps(200000L, 0L));
        UserCacheVo user = new UserCacheVo();
        user.setId(1L);
        user.setActiveTenantId(100L);
        when(userFeign.getLocalUser()).thenReturn(R.ok(user));
        com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo video = new com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo();
        video.setVideoId("v1");
        video.setTenantId(100L);
        video.setUserId(1L);
        video.setExistBarrage(0);
        when(anchorVideoFeign.GetByVideoId(anyString())).thenReturn(R.ok(video));
        when(aiTokenWithholdFeign.withholdAiToken(anyLong(), anyLong()))
                .thenReturn(new com.jiuyu.replay.generic.vo.order.RedisWithholdVo());
        when(rocketMqBll.syncSendAndDeliverToTopic(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);

        // when：手动触发走 triggerReport
        TriggerReportBo bo = new TriggerReportBo();
        bo.setSourceType(0);
        // HOTFIX 2026-06-06: sourceType=0 录制视频对应 sceneType=0 复盘场景（合法组合）
        bo.setSceneType(0);
        bo.setSourceId("v1");
        bo.setMonitorType(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        bll.triggerReport(bo);

        // then：handleExistingAndReturn 用 lambdaUpdate 实现重置（DB 写入），同步 existing 内存为 manual
        verify(updateChain).update();
        assertThat(existing.getTriggerSource()).isEqualTo("manual");
    }
}
