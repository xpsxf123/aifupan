package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.service.aiagent.AiAgentInfluencerVideoService;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.generic.bo.aiagent.HotSearchAnalyticsOverviewBo;
import com.jiuyu.replay.generic.bo.aiagent.HotSearchKeywordQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.HotSearchVideoQueryBo;
import com.jiuyu.replay.generic.vo.aiagent.HotSearchAnalyticsVo;
import com.jiuyu.replay.generic.vo.aiagent.HotSearchKeywordVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoItemVo;
import com.jiuyu.replay.generic.vo.aiagent.StreamPageVo;
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

/**
 * AI Agent 爆款开放接口（只读查询，API-Key 鉴权）。
 *
 * <p>面向"通过爆款关键词筛选参考短视频"场景：爆款关键词 + 关键词下短视频（标题/#标签/指标过滤，keyset 游标流式分页）。
 * 爆款库为公共爬取数据、无 tenant_id。</p>
 *
 * @author fupan-server
 */
@Slf4j
@Validated
@RestController
@APIKey
@AllArgsConstructor
@RequestMapping("/internal/ai-agent/hotsearch")
@Tag(name = "AI Agent 爆款开放接口")
public class AiAgentHotSearchController {

    private static final String LOG_PREFIX = "[AI-AGENT][INBOUND]";

    private final AiAgentInfluencerVideoService aiAgentInfluencerVideoService;

    /**
     * 爆款关键词列表（全库，keyset 游标流式分页）。
     *
     * @param bo 查询条件
     * @return 爆款关键词页
     */
    @PostMapping("/keyword/list")
    @Operation(summary = "爆款关键词列表")
    public R<StreamPageVo<HotSearchKeywordVo>> keywordList(@Valid @RequestBody HotSearchKeywordQueryBo bo) {
        log.info("{} hotsearch/keyword/list, tenantId={}, keyword={}", LOG_PREFIX, bo.getTenantId(), bo.getKeyword());
        return aiAgentInfluencerVideoService.hotSearchKeywordList(bo);
    }

    /**
     * 爆款视频列表（某爆款关键词下的短视频，按标题/#标签/指标过滤，keyset 游标流式分页）。
     *
     * @param bo 查询条件（含 searchId）
     * @return 短视频页
     */
    @PostMapping("/video/list")
    @Operation(summary = "爆款视频列表")
    public R<StreamPageVo<InfluencerVideoItemVo>> videoList(@Valid @RequestBody HotSearchVideoQueryBo bo) {
        log.info("{} hotsearch/video/list, tenantId={}, searchId={}", LOG_PREFIX, bo.getTenantId(), bo.getSearchId());
        return aiAgentInfluencerVideoService.hotSearchVideoList(bo);
    }

    /**
     * 爆款选题拆解聚合（某爆款关键词下的选题分析，服务端一次算完，前端只渲染）。
     *
     * @param bo 查询条件（含 searchId + 统计时间窗）
     * @return 选题拆解聚合结果
     */
    @PostMapping("/analytics/overview")
    @Operation(summary = "爆款选题拆解聚合")
    public R<HotSearchAnalyticsVo> hotSearchAnalyticsOverview(@Valid @RequestBody HotSearchAnalyticsOverviewBo bo) {
        log.info("{} hotsearch/analytics/overview, tenantId={}, searchId={}, platformType={}", LOG_PREFIX,
                bo.getTenantId(), bo.getSearchId(), bo.getPlatformType());
        return aiAgentInfluencerVideoService.hotSearchAnalyticsOverview(bo);
    }
}
