package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.service.aiagent.AiAgentInfluencerVideoService;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerAnalyticsOverviewBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerDetailBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerListQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerSearchBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerVideoStatsBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoAudioTextBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoDetailBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoGlobalSearchBo;
import com.jiuyu.replay.generic.vo.aiagent.CursorPageVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerAnalyticsVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerDetailBatchVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerItemVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoItemVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoStatsVo;
import com.jiuyu.replay.generic.vo.aiagent.StreamPageVo;
import com.jiuyu.replay.generic.vo.aiagent.VideoAudioTextVo;
import com.jiuyu.replay.generic.vo.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI Agent 达人短视频开放接口（只读查询，API-Key 鉴权）。
 *
 * <p>面向 AI Agent 项目以 tool 形式调用：不依赖 JWT，身份三件套（userId/tenantId/userType）由请求体显式携带。
 * 达人范围取租户达人订阅，短视频范围取租户已采集视频，均按 tenantId 隔离（子账号叠加 userId）。
 * 列表接口走游标流式分页，过滤条件尽量支持 in 集合，方便 Agent 批量拉取。</p>
 *
 * @author fupan-server
 */
@Slf4j
@Validated
@RestController
@APIKey
@AllArgsConstructor
@RequestMapping("/internal/ai-agent/influencer")
@Tag(name = "AI Agent 达人短视频开放接口")
public class AiAgentInfluencerVideoController {

    private static final String LOG_PREFIX = "[AI-AGENT][INBOUND]";

    private final AiAgentInfluencerVideoService aiAgentInfluencerVideoService;

    /**
     * 达人列表（租户达人订阅维度，附短视频数量 + 最近采集时间，游标分页）。
     *
     * @param bo 查询条件
     * @return 达人列表游标页
     */
    @PostMapping("/list")
    @Operation(summary = "达人列表")
    public R<CursorPageVo<InfluencerItemVo>> influencerList(@Valid @RequestBody InfluencerListQueryBo bo) {
        log.info("{} influencer/list, tenantId={}, userType={}", LOG_PREFIX, bo.getTenantId(), bo.getUserType());
        return aiAgentInfluencerVideoService.influencerList(bo);
    }

    /**
     * 批量获取达人短视频原文音频转文字内容（按 tb_video_info id 直查）。
     *
     * @param bo 查询条件（含 videoIdList）
     * @return 原文内容列表
     */
    @PostMapping("/video/audio-text/batch")
    @Operation(summary = "批量获取短视频原文音频转文字")
    public R<List<VideoAudioTextVo>> batchVideoAudioText(@Valid @RequestBody VideoAudioTextBatchBo bo) {
        log.info("{} influencer/video/audio-text/batch, tenantId={}, size={}", LOG_PREFIX, bo.getTenantId(),
                bo.getVideoIdList() == null ? 0 : bo.getVideoIdList().size());
        return aiAgentInfluencerVideoService.batchVideoAudioText(bo);
    }

    /**
     * 批量达人短视频统计（仅租户已采集：数量 + 点赞/评论/分享/收藏汇总）。
     *
     * @param bo 查询条件（含 influencerIdList）
     * @return 统计列表
     */
    @PostMapping("/video/stats/batch")
    @Operation(summary = "批量达人短视频统计")
    public R<List<InfluencerVideoStatsVo>> batchInfluencerVideoStats(@Valid @RequestBody InfluencerVideoStatsBatchBo bo) {
        log.info("{} influencer/video/stats/batch, tenantId={}, size={}", LOG_PREFIX, bo.getTenantId(),
                bo.getInfluencerIdList() == null ? 0 : bo.getInfluencerIdList().size());
        return aiAgentInfluencerVideoService.batchInfluencerVideoStats(bo);
    }

    /**
     * 短视频检索（按达人/关键词/#标签/指标过滤，keyset 游标流式分页）。
     *
     * <p>{@code collectedOnly=false}（默认）=跨租户读公共爬取全库；{@code collectedOnly=true}=仅限本租户已采集范围
     * （JOIN tb_video_user_video 在库内过滤，keyword/排序/keyset 分页等参数与全库完全一致）。原「达人短视频列表（仅已采集）」
     * 独立接口已由此开关合并替代。</p>
     *
     * @param bo 查询条件
     * @return 短视频页
     */
    @PostMapping("/video/global/search")
    @Operation(summary = "短视频检索（全库 / 仅已采集）")
    public R<StreamPageVo<InfluencerVideoItemVo>> globalVideoSearch(@Valid @RequestBody VideoGlobalSearchBo bo) {
        log.info("{} influencer/video/global/search, tenantId={}, keyword={}", LOG_PREFIX, bo.getTenantId(), bo.getKeyword());
        return aiAgentInfluencerVideoService.globalVideoSearch(bo);
    }

    /**
     * 批量短视频完整明细（按 tb_video_info id 直查，可选加载原文音频转文字）。
     *
     * @param bo 查询条件（含 videoIdList）
     * @return 明细列表
     */
    @PostMapping("/video/detail/batch")
    @Operation(summary = "批量短视频完整明细")
    public R<List<InfluencerVideoItemVo>> videoDetailBatch(@Valid @RequestBody VideoDetailBatchBo bo) {
        log.info("{} influencer/video/detail/batch, tenantId={}, size={}", LOG_PREFIX, bo.getTenantId(),
                bo.getVideoIdList() == null ? 0 : bo.getVideoIdList().size());
        return aiAgentInfluencerVideoService.videoDetailBatch(bo);
    }

    /**
     * 全库达人搜索（跨租户读公共爬取库，按昵称/账号/平台/粉丝过滤，keyset 游标流式分页）。
     *
     * @param bo 查询条件
     * @return 达人页
     */
    @PostMapping("/search")
    @Operation(summary = "全库达人搜索")
    public R<StreamPageVo<InfluencerItemVo>> influencerSearch(@Valid @RequestBody InfluencerSearchBo bo) {
        log.info("{} influencer/search, tenantId={}, keyword={}", LOG_PREFIX, bo.getTenantId(), bo.getKeyword());
        return aiAgentInfluencerVideoService.influencerSearch(bo);
    }

    /**
     * 达人统计概览（单达人档案下钻，服务端一次聚合 KPI/雷达/趋势/热力/时长/爆款；见设计 doc17 §6.20）。
     *
     * @param bo 查询条件（含 influencerId、scope、统计时间窗）
     * @return 统计概览
     */
    @PostMapping("/analytics/overview")
    @Operation(summary = "达人统计概览")
    public R<InfluencerAnalyticsVo> influencerAnalyticsOverview(@Valid @RequestBody InfluencerAnalyticsOverviewBo bo) {
        log.info("{} influencer/analytics/overview, tenantId={}, influencerId={}, scope={}", LOG_PREFIX,
                bo.getTenantId(), bo.getInfluencerId(), bo.getScope());
        return aiAgentInfluencerVideoService.influencerAnalyticsOverview(bo);
    }

    /**
     * 批量达人档案（按 author_id 直查公共爬取达人库，不分页）。
     *
     * @param bo 查询条件（含 authorIdList）
     * @return 达人档案 + 未命中 id
     */
    @PostMapping("/detail/batch")
    @Operation(summary = "批量达人档案")
    public R<InfluencerDetailBatchVo> influencerDetailBatch(@Valid @RequestBody InfluencerDetailBatchBo bo) {
        log.info("{} influencer/detail/batch, tenantId={}, size={}", LOG_PREFIX,
                bo.getTenantId(), bo.getAuthorIdList() == null ? 0 : bo.getAuthorIdList().size());
        return aiAgentInfluencerVideoService.influencerDetailBatch(bo);
    }
}
