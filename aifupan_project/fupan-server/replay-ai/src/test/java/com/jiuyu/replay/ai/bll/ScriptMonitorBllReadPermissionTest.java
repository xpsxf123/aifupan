package com.jiuyu.replay.ai.bll;

import com.jiuyu.replay.ai.config.ScriptMonitorMqProperties;
import com.jiuyu.replay.ai.entity.ScriptMonitorReportEntity;
import com.jiuyu.replay.ai.repository.mongo.ScriptMonitorReportBodyRepository;
import com.jiuyu.replay.ai.repository.service.ScriptMonitorReportService;
import com.jiuyu.replay.ai.vo.FidelityReportDetailVo;
import com.jiuyu.replay.ai.vo.InteractionPatrolReportDetailVo;
import com.jiuyu.replay.ai.vo.QualityReportDetailVo;
import com.jiuyu.replay.common.bll.RocketMqBll;
import com.jiuyu.replay.generic.enums.words.MonitorSourceTypeEnum;
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
import com.jiuyu.replay.generic.vo.power.UserCacheVo;
import com.jiuyu.replay.generic.vo.words.AnchorVideoInfoVo;
import com.jiuyu.replay.generic.vo.words.UploadFileSimpleInfoVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ScriptMonitorBll 三类 detail 接口读权限放宽到云空间分享 - 单元测试
 *
 * <p>对应需求 {@code .claude/runs/Change__shared-report-readable__2026-06-29_15-25-42}：
 * qualityReportDetail / patrolReportDetail / fidelityReportDetail 三个接口
 * 把读权限从「同租户独占」放宽到「同租户 OR video.uploadStatus=1 云空间公开分享」。</p>
 *
 * <p>测试矩阵 = 3 detail × 7 AC = 21 个 case：</p>
 * <ul>
 *   <li>AC-1 同租户基准：放权前能读，放权后必须仍能读（回归）</li>
 *   <li>AC-2 跨租户 uploadStatus=1：新增正向通路（仅读，不写已读）</li>
 *   <li>AC-3 跨租户 uploadStatus=0：拒绝（保留 70011）</li>
 *   <li>AC-4 跨租户 video=null：拒绝（保留 70011）</li>
 *   <li>AC-5 跨租户 sourceType=1 上传文件：拒绝（uploadStatus 不参与桥接）</li>
 *   <li>AC-6 同租户非录制人：写已读 guard 未退化</li>
 *   <li>AC-7 报告不存在：先抛 NOT_EXIST，不触达权限分支</li>
 * </ul>
 *
 * <p>不连真实 DB/Redis/MQ；用 @Mock + 构造器手工注入。</p>
 *
 * @author beta
 * @date 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScriptMonitorBllReadPermissionTest {

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

    /**
     * 当前用户：id=1, activeTenantId=100。所有测试基线一致。
     * 通过 user.activeTenantId vs report.tenantId / video.tenantId 控制同租户/跨租户分支。
     */
    private UserCacheVo user;

    @BeforeEach
    void setUp() {
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

        user = new UserCacheVo();
        user.setId(1L);
        user.setActiveTenantId(100L);
        when(userFeign.getLocalUser()).thenReturn(R.ok(user));
    }

    // ---------- helpers ----------

    /**
     * 构造视频：tenantId/userId/uploadStatus 三参数控制权限分支。
     */
    private AnchorVideoInfoVo video(Long tenantId, Long userId, Integer uploadStatus) {
        AnchorVideoInfoVo v = new AnchorVideoInfoVo();
        v.setVideoId("v1");
        v.setTenantId(tenantId);
        v.setUserId(userId);
        v.setUploadStatus(uploadStatus);
        v.setLiveTitle("直播间标题");
        v.setUserNickName("主播昵称");
        return v;
    }

    /**
     * 构造报告：默认 sourceType=0 录制视频 / sourceId="v1" / tenantId=100 同租户 / isDeleted=0 / isRead=0。
     * 测试按需覆盖 tenantId / sourceType / isRead 等字段。
     */
    private ScriptMonitorReportEntity report(Integer monitorType) {
        ScriptMonitorReportEntity r = new ScriptMonitorReportEntity();
        r.setId(5L);
        r.setTenantId(100L);
        r.setSourceType(MonitorSourceTypeEnum.RECORD_VIDEO.getCode());
        r.setSceneType(0);
        r.setSourceId("v1");
        r.setMonitorType(monitorType);
        r.setStatus(ScriptMonitorStatusEnum.GENERATED.getCode());
        r.setIsDeleted(0);
        r.setIsRead(0);
        return r;
    }

    /**
     * 上传文件 fileInfo（sourceType=1 时 loadSingleVideo 通过 sensitiveWordsFeign 取）。
     * uploadStatus 不参与桥接（loadSingleVideo 只填 tenantId/userId），所以 sourceType=1 跨租户必拒。
     */
    private UploadFileSimpleInfoVo uploadFile(Long tenantId, Long userId) {
        UploadFileSimpleInfoVo f = new UploadFileSimpleInfoVo();
        f.setFileId("file1");
        f.setTenantId(tenantId);
        f.setUserId(userId);
        f.setFileName("上传文件.mp4");
        f.setUploadTime("2026-06-01 10:00:00");
        f.setFileType(0);
        return f;
    }

    // ==================== qualityReportDetail ====================

    @Test
    @DisplayName("AC-1 quality 同租户录制人首次查看：返回完整 VO 且触发 markReadIfNeeded")
    void qualityReportDetail_sameTenantRecorder_returnsVoAndMarksRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        // tenantId=100 同 user.activeTenantId=100；video.userId=1 同 user.id=1 → 录制人
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());
        doNothing().when(writeService).markReadIfNeeded(eq(5L), eq(100L));

        QualityReportDetailVo vo = bll.qualityReportDetail(5L);

        assertNotNull(vo);
        assertEquals(5L, vo.getReportId());
        verify(writeService).markReadIfNeeded(eq(5L), eq(100L));
    }

    @Test
    @DisplayName("AC-2 quality 跨租户 uploadStatus=1 已分享：放行返回 VO 且不触发 markReadIfNeeded")
    void qualityReportDetail_crossTenantPublicShared_returnsVoAndDoesNotMarkRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setTenantId(999L); // 跨租户
        when(reportService.getById(5L)).thenReturn(r);
        // video.tenantId=999 / userId=999（非当前 user）/ uploadStatus=1 已公开分享
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 999L, 1)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        QualityReportDetailVo vo = bll.qualityReportDetail(5L);

        assertNotNull(vo);
        assertEquals(5L, vo.getReportId());
        // AC-2 核心：跨租户场景即使读到，也不能写已读（非录制人 + 跨租户 tenantId 错位）
        verify(writeService, never()).markReadIfNeeded(any(), any());
    }

    @Test
    @DisplayName("AC-3 quality 跨租户 uploadStatus=0 未分享：抛 70011")
    void qualityReportDetail_crossTenantUploadStatusZero_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 999L, 0)));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.qualityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("AC-4 quality 跨租户 video=null（视频不存在）：抛 70011")
    void qualityReportDetail_crossTenantVideoNull_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        // anchorVideoFeign 返 null → loadSingleVideo 返 null → publicShared=false → 拒
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok((AnchorVideoInfoVo) null));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.qualityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("AC-5 quality 跨租户 sourceType=1 上传文件：抛 70011（uploadStatus 不参与桥接）")
    void qualityReportDetail_crossTenantUploadFile_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        r.setTenantId(999L);
        r.setSourceType(MonitorSourceTypeEnum.UPLOAD_FILE.getCode());
        r.setSourceId("file1");
        when(reportService.getById(5L)).thenReturn(r);
        // sourceType=1 走 sensitiveWordsFeign 桥接：只填 tenantId/userId，uploadStatus 恒 null → publicShared=false
        when(sensitiveWordsFeign.getUploadFileInfo("file1")).thenReturn(R.ok(uploadFile(999L, 999L)));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.qualityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("AC-6 quality 同租户非录制人：返回 VO 但不触发 markReadIfNeeded")
    void qualityReportDetail_sameTenantNonRecorder_doesNotMarkRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.QUALITY_INSPECTION.getCode());
        // tenantId=100 同租户；video.userId=999 != user.id=1 → 非录制人
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 999L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        QualityReportDetailVo vo = bll.qualityReportDetail(5L);

        assertNotNull(vo);
        verify(writeService, never()).markReadIfNeeded(any(), any());
        assertEquals(0, vo.getIsRead());
    }

    @Test
    @DisplayName("AC-7 quality 报告不存在：先抛 NOT_EXIST 不触达权限分支")
    void qualityReportDetail_reportNotExist_throwsNotExistBeforePermission() {
        when(reportService.getById(5L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.qualityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST.getCode(), ex.getCode());
        // 权限分支必须未触达（不应调 anchorVideoFeign）
        verify(anchorVideoFeign, never()).GetByVideoId(any());
    }

    // ==================== patrolReportDetail ====================

    @Test
    @DisplayName("AC-1 patrol 同租户录制人首次查看：返回完整 VO 且触发 markReadIfNeeded")
    void patrolReportDetail_sameTenantRecorder_returnsVoAndMarksRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.INTERACTION_PATROL.getCode());
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());
        doNothing().when(writeService).markReadIfNeeded(eq(5L), eq(100L));

        InteractionPatrolReportDetailVo vo = bll.patrolReportDetail(5L);

        assertNotNull(vo);
        assertEquals(5L, vo.getReportId());
        verify(writeService).markReadIfNeeded(eq(5L), eq(100L));
    }

    @Test
    @DisplayName("AC-2 patrol 跨租户 uploadStatus=1 已分享：放行返回 VO 且不触发 markReadIfNeeded")
    void patrolReportDetail_crossTenantPublicShared_returnsVoAndDoesNotMarkRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.INTERACTION_PATROL.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 999L, 1)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        InteractionPatrolReportDetailVo vo = bll.patrolReportDetail(5L);

        assertNotNull(vo);
        assertEquals(5L, vo.getReportId());
        verify(writeService, never()).markReadIfNeeded(any(), any());
    }

    @Test
    @DisplayName("AC-3 patrol 跨租户 uploadStatus=0 未分享：抛 70011")
    void patrolReportDetail_crossTenantUploadStatusZero_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.INTERACTION_PATROL.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 999L, 0)));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.patrolReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("AC-4 patrol 跨租户 video=null（视频不存在）：抛 70011")
    void patrolReportDetail_crossTenantVideoNull_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.INTERACTION_PATROL.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok((AnchorVideoInfoVo) null));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.patrolReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("AC-5 patrol 跨租户 sourceType=1 上传文件：抛 70011（uploadStatus 不参与桥接）")
    void patrolReportDetail_crossTenantUploadFile_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.INTERACTION_PATROL.getCode());
        r.setTenantId(999L);
        r.setSourceType(MonitorSourceTypeEnum.UPLOAD_FILE.getCode());
        r.setSourceId("file1");
        when(reportService.getById(5L)).thenReturn(r);
        when(sensitiveWordsFeign.getUploadFileInfo("file1")).thenReturn(R.ok(uploadFile(999L, 999L)));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.patrolReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("AC-6 patrol 同租户非录制人：返回 VO 但不触发 markReadIfNeeded")
    void patrolReportDetail_sameTenantNonRecorder_doesNotMarkRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.INTERACTION_PATROL.getCode());
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 999L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        InteractionPatrolReportDetailVo vo = bll.patrolReportDetail(5L);

        assertNotNull(vo);
        verify(writeService, never()).markReadIfNeeded(any(), any());
        assertEquals(0, vo.getIsRead());
    }

    @Test
    @DisplayName("AC-7 patrol 报告不存在：先抛 NOT_EXIST 不触达权限分支")
    void patrolReportDetail_reportNotExist_throwsNotExistBeforePermission() {
        when(reportService.getById(5L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.patrolReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST.getCode(), ex.getCode());
        verify(anchorVideoFeign, never()).GetByVideoId(any());
    }

    // ==================== fidelityReportDetail ====================

    @Test
    @DisplayName("AC-1 fidelity 同租户录制人首次查看：返回完整 VO 且触发 markReadIfNeeded")
    void fidelityReportDetail_sameTenantRecorder_returnsVoAndMarksRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 1L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());
        doNothing().when(writeService).markReadIfNeeded(eq(5L), eq(100L));

        FidelityReportDetailVo vo = bll.fidelityReportDetail(5L);

        assertNotNull(vo);
        assertEquals(5L, vo.getReportId());
        verify(writeService).markReadIfNeeded(eq(5L), eq(100L));
    }

    @Test
    @DisplayName("AC-2 fidelity 跨租户 uploadStatus=1 已分享：放行返回 VO 且不触发 markReadIfNeeded")
    void fidelityReportDetail_crossTenantPublicShared_returnsVoAndDoesNotMarkRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 999L, 1)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        FidelityReportDetailVo vo = bll.fidelityReportDetail(5L);

        assertNotNull(vo);
        assertEquals(5L, vo.getReportId());
        verify(writeService, never()).markReadIfNeeded(any(), any());
    }

    @Test
    @DisplayName("AC-3 fidelity 跨租户 uploadStatus=0 未分享：抛 70011")
    void fidelityReportDetail_crossTenantUploadStatusZero_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(999L, 999L, 0)));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.fidelityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("AC-4 fidelity 跨租户 video=null（视频不存在）：抛 70011")
    void fidelityReportDetail_crossTenantVideoNull_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setTenantId(999L);
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok((AnchorVideoInfoVo) null));

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.fidelityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("AC-5 fidelity 跨租户 sourceType=1 上传文件：抛 70011（log.warn 兜底分支 video 恒 null）")
    void fidelityReportDetail_crossTenantUploadFile_throwsNoPermission() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        r.setTenantId(999L);
        r.setSourceType(MonitorSourceTypeEnum.UPLOAD_FILE.getCode());
        r.setSourceId("file1");
        when(reportService.getById(5L)).thenReturn(r);
        // fidelity sourceType=1 走 log.warn 分支：video 强制 null → 不走 sensitiveWordsFeign 桥接

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.fidelityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_NO_PERMISSION.getCode(), ex.getCode());
        // 关键回归：fidelity sourceType=1 不应调 sensitiveWordsFeign（异常数据分支）
        verify(sensitiveWordsFeign, never()).getUploadFileInfo(any());
    }

    @Test
    @DisplayName("AC-6 fidelity 同租户非录制人：返回 VO 但不触发 markReadIfNeeded")
    void fidelityReportDetail_sameTenantNonRecorder_doesNotMarkRead() {
        ScriptMonitorReportEntity r = report(MonitorTypeEnum.FIDELITY_MONITOR.getCode());
        when(reportService.getById(5L)).thenReturn(r);
        when(anchorVideoFeign.GetByVideoId("v1")).thenReturn(R.ok(video(100L, 999L, 0)));
        when(reportBodyRepository.findByReportId(5L)).thenReturn(Optional.empty());

        FidelityReportDetailVo vo = bll.fidelityReportDetail(5L);

        assertNotNull(vo);
        verify(writeService, never()).markReadIfNeeded(any(), any());
        assertEquals(0, vo.getIsRead());
    }

    @Test
    @DisplayName("AC-7 fidelity 报告不存在：先抛 NOT_EXIST 不触达权限分支")
    void fidelityReportDetail_reportNotExist_throwsNotExistBeforePermission() {
        when(reportService.getById(5L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> bll.fidelityReportDetail(5L));
        assertEquals(StatusCode.SCRIPT_MONITOR_REPORT_NOT_EXIST.getCode(), ex.getCode());
        verify(anchorVideoFeign, never()).GetByVideoId(any());
    }
}
