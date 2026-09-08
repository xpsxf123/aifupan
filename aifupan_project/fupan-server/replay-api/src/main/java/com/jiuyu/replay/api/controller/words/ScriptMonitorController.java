package com.jiuyu.replay.api.controller.words;

import com.jiuyu.replay.ai.bll.ScriptMonitorBll;
import com.jiuyu.replay.ai.bo.BatchReportStatusBo;
import com.jiuyu.replay.ai.bo.ReportStatusBo;
import com.jiuyu.replay.ai.bo.TriggerReportBo;
import com.jiuyu.replay.ai.vo.FidelityReportDetailVo;
import com.jiuyu.replay.ai.vo.InteractionPatrolReportDetailVo;
import com.jiuyu.replay.ai.vo.QualityReportDetailVo;
import com.jiuyu.replay.ai.vo.ScriptMonitorReportStatusVo;
import com.jiuyu.replay.common.aspect.lock.repeatsubmit.NoRepeatSubmit;
import com.jiuyu.replay.generic.feign.words.AnchorUrlUserFeign;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.AnchorUrlUserVo;
import com.jiuyu.replay.power.utils.GlobalObject;
import com.jiuyu.replay.power.vo.UserCacheVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 话术智能监控接口（B4 质检 / B5 监控位 / B7 自动触发 / B8 toggle 开关）。
 *
 * <p>提供质检 / 还原度 / 互动巡检报告的状态查询、手动 triggerReport、
 * 直播间配置基础查询（含 3 开关）和单能力 toggle 开关入口。已读由 detail 接口内置写入。</p>
 *
 * @author beta
 * @date 2026-05-29
 */
@RestController
@CrossOrigin
@RequestMapping("replay/script-monitor")
@Tag(name = "话术智能监控")
@AllArgsConstructor
public class ScriptMonitorController {

    private final ScriptMonitorBll scriptMonitorBll;
    private final AnchorUrlUserFeign anchorUrlUserFeign;

    /**
     * 查询单个资源的三类监控报告状态
     *
     * <p>2026-06-02 起契约变更：GET → POST + Body，跟 batchReportStatus 风格对齐；
     * 增加可选字段 {@code secUid}，传入后响应 monitors[].monitorEnabled 会按
     * (userId, tenantId, secUid) 查 tb_anchor_url_user 三开关。</p>
     *
     * @param bo 报告状态查询请求（sourceType/sceneType/sourceId/secUid?）
     * @return 报告状态
     */
    @PostMapping("/reportStatus")
    @Operation(summary = "查询单个资源的报告状态")
    public R<ScriptMonitorReportStatusVo> reportStatus(@Validated @RequestBody ReportStatusBo bo) {
        return R.ok(scriptMonitorBll.reportStatus(bo));
    }

    /**
     * 批量查询报告状态，跳过无权限/不存在的资源
     *
     * @param bo 资源列表，最多 100 条
     * @return 报告状态列表
     */
    @PostMapping("/batchReportStatus")
    @Operation(summary = "批量查询报告状态(最多100)")
    public R<List<ScriptMonitorReportStatusVo>> batchReportStatus(@Validated @RequestBody BatchReportStatusBo bo) {
        return R.ok(scriptMonitorBll.batchReportStatus(bo));
    }

    /**
     * 查询质检报告详情
     *
     * <p>同租户可读；跨租户仅当对应视频已上传云空间（uploadStatus=1）时可读，否则抛 70011。
     * 录制人本人首次查看时自动写已读，分享接收方查看不写。</p>
     *
     * @param reportId 报告ID
     * @return 质检报告详情（含报告正文、主播信息、已读状态）
     */
    @GetMapping("/qualityReportDetail")
    @Operation(summary = "查询质检报告详情")
    public R<QualityReportDetailVo> qualityReportDetail(@RequestParam Long reportId) {
        return R.ok(scriptMonitorBll.qualityReportDetail(reportId));
    }

    /**
     * 查询互动巡检报告详情
     *
     * <p>同租户可读；跨租户仅当对应视频已上传云空间（uploadStatus=1）时可读，否则抛 70011。
     * 录制人本人查看时自动写已读，分享接收方查看不写。</p>
     *
     * @param reportId 报告 ID
     * @return 互动巡检报告详情
     */
    @GetMapping("/patrolReportDetail")
    @Operation(summary = "查询互动巡检报告详情")
    public R<InteractionPatrolReportDetailVo> patrolReportDetail(@RequestParam Long reportId) {
        return R.ok(scriptMonitorBll.patrolReportDetail(reportId));
    }

    /**
     * 查询话术还原度报告详情
     *
     * <p>仅支持 sourceType=0 录制视频场景。录制人首次查看时自动写已读（独立事务 + fail-safe）。
     * 同租户可读；跨租户仅当对应视频已上传云空间（uploadStatus=1）时可读，否则抛 70011。</p>
     *
     * @param reportId 报告 ID
     * @return 话术还原度报告详情（13 字段，含摘要 Markdown + 正文 + 主播信息 + 已读状态）
     */
    @GetMapping("/fidelityReportDetail")
    @Operation(summary = "查询话术还原度报告详情")
    public R<FidelityReportDetailVo> fidelityReportDetail(@RequestParam Long reportId) {
        return R.ok(scriptMonitorBll.fidelityReportDetail(reportId));
    }

    /**
     * 手动触发报告生成
     *
     * @param bo    资源定位（sourceType/sceneType/sourceId）+ 监控类型
     * @param token 用户身份 Token（用于防重提交 key 区分不同用户，由请求头自动注入）
     * @return 是否成功
     */
    @PostMapping("/triggerReport")
    @Operation(summary = "手动触发报告生成")
    @NoRepeatSubmit(key = "#bo.sourceType + '_' + #bo.sceneType + '_' + #bo.sourceId + '_' + #bo.monitorType + '_' + #token",
            message = "请勿重复触发，请稍后再试")
    public R<Boolean> triggerReport(@Validated @RequestBody TriggerReportBo bo,
                                    @RequestHeader(value = "Token", required = false) String token) {
        scriptMonitorBll.triggerReport(bo);
        return R.ok(true);
    }

    /**
     * 查询直播间基础配置（话术智能监控开关 + 标准稿状态）
     *
     * <p>按当前登录用户 + 租户 + secUid 查询 tb_anchor_url_user 记录，
     * 返回含话术质检/话术还原度/互动巡检三类开关及标准稿ID（B5 阶段恒为 null）。</p>
     *
     * @param secUid 主播唯一标识
     * @return 直播间基础配置信息
     */
    @GetMapping("/anchorBasicConfig")
    @Operation(summary = "查询直播间基础配置")
    public R<AnchorUrlUserVo> anchorBasicConfig(@RequestParam String secUid) {
        return scriptMonitorBll.anchorBasicConfig(secUid);
    }

    /**
     * 切换单个 AI 监控能力开关
     *
     * <p>按「短路幂等 → 还原度暂禁 → 授权量校验 → 单字段写库 → 占用更新」完成单能力开关切换。
     * 当前状态与目标状态相同时（幂等场景），直接返回 true 不写库。</p>
     *
     * @param secUid      主播唯一标识
     * @param monitorType 监控类型 0=话术质检 / 1=话术还原度 / 2=互动巡检
     * @param enabled     目标状态 0=关闭 / 1=开启
     * @param token       用户身份 Token（用于防重提交 key 区分不同用户，由请求头自动注入）
     * @return 切换结果，true 表示成功（含幂等情况）
     */
    @NoRepeatSubmit(key = "#secUid + '_' + #monitorType + '_' + #enabled + '_' + #token",
            message = "请勿重复提交，请稍后再试")
    @PostMapping("/setMonitorEnabled")
    @Operation(summary = "切换单个 AI 监控能力开关")
    public R<Boolean> setMonitorEnabled(@RequestParam("secUid") String secUid,
                                        @RequestParam("monitorType") Integer monitorType,
                                        @RequestParam("enabled") Integer enabled,
                                        @RequestHeader(value = "Token", required = false) String token) {
        UserCacheVo user = GlobalObject.getLocalUser();
        return anchorUrlUserFeign.setMonitorEnabled(user.getId(), user.getActiveTenantId(), secUid, monitorType, enabled);
    }
}
