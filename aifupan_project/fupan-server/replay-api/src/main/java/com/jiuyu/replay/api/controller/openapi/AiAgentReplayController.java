package com.jiuyu.replay.api.controller.openapi;

import com.jiuyu.replay.api.logic.words.BlessBagLogic;
import com.jiuyu.replay.api.service.aiagent.AiAgentReplayService;
import com.jiuyu.replay.common.open.APIKey;
import com.jiuyu.replay.generic.bo.aiagent.AiAgentBaseBo;
import com.jiuyu.replay.generic.bo.aiagent.AnchorDetailQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.AnchorListQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.BarrageQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.BlessBagListQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.OnlineCurveQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.SensitiveWordsQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.TradeOptionsQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.TradeParentsQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoListQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoQueryBo;
import com.jiuyu.replay.generic.vo.aiagent.*;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.generic.vo.words.BlessBagListVo;
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
 * AI Agent 复盘数据开放接口（只读查询，API-Key 鉴权）。
 *
 * <p>面向 AI Agent 项目以 tool 形式调用：不依赖 JWT，身份三件套（userId/tenantId/userType）由请求体显式携带，
 * 服务端据此做租户隔离 + 数据范围裁剪。列表接口走游标流式分页。</p>
 *
 * @author fupan-server
 */
@Slf4j
@Validated
@RestController
@APIKey
@AllArgsConstructor
@RequestMapping("/internal/ai-agent")
@Tag(name = "AI Agent 复盘数据开放接口")
public class AiAgentReplayController {

    private static final String LOG_PREFIX = "[AI-AGENT][INBOUND]";

    private final AiAgentReplayService aiAgentReplayService;

    private final BlessBagLogic blessBagLogic;

    /**
     * 主播列表（多条件过滤 + 游标分页）。
     *
     * @param bo 查询条件
     * @return 主播列表游标页
     */
    @PostMapping("/anchor/list")
    @Operation(summary = "主播列表")
    public R<CursorPageVo<AnchorItemVo>> anchorList(@Valid @RequestBody AnchorListQueryBo bo) {
        log.info("{} anchor/list, tenantId={}, userType={}", LOG_PREFIX, bo.getTenantId(), bo.getUserType());
        return aiAgentReplayService.anchorList(bo);
    }

    /**
     * 主播行业聚合（ID + 名称去重）。
     *
     * @param bo 身份
     * @return 行业聚合列表
     */
    @PostMapping("/anchor/trade-options")
    @Operation(summary = "主播行业聚合去重")
    public R<List<TradeOptionVo>> tradeOptions(@Valid @RequestBody TradeOptionsQueryBo bo) {
        log.info("{} anchor/trade-options, tenantId={}, userType={}", LOG_PREFIX, bo.getTenantId(), bo.getUserType());
        return aiAgentReplayService.tradeOptions(bo);
    }

    /**
     * 行业层级链（自身 + 各级父行业），按层级排序，离 tradeId 越近越靠前。
     *
     * @param bo 查询条件（含 tradeId）
     * @return 行业层级链（id + name + parentId）
     */
    @PostMapping("/anchor/trade-parents")
    @Operation(summary = "行业层级链（父行业）")
    public R<List<TradeParentVo>> tradeParents(@Valid @RequestBody TradeParentsQueryBo bo) {
        log.info("{} anchor/trade-parents, tenantId={}, tradeId={}", LOG_PREFIX, bo.getTenantId(), bo.getTradeId());
        return aiAgentReplayService.tradeParents(bo);
    }

    /**
     * 行业敏感词库（按行业父链取词：4 级自身 + 3/2/1 级父行业 + 全行业），供输出话术前合规自检。
     *
     * @param bo 查询条件（含 tradeId，可选 platform / wordsType）
     * @return 敏感词条目（按严重度升序）
     */
    @PostMapping("/anchor/sensitive-words")
    @Operation(summary = "行业敏感词库（父链取词）")
    public R<List<SensitiveWordVo>> sensitiveWords(@Valid @RequestBody SensitiveWordsQueryBo bo) {
        log.info("{} anchor/sensitive-words, tenantId={}, tradeId={}, platform={}", LOG_PREFIX, bo.getTenantId(), bo.getTradeId(), bo.getPlatform());
        return aiAgentReplayService.sensitiveWords(bo);
    }

    /**
     * 主播详情（基础 + 配置 + 基础设置，字典已解析）。
     *
     * @param bo 查询条件（含 secUid）
     * @return 主播详情
     */
    @PostMapping("/anchor/detail")
    @Operation(summary = "主播详情")
    public R<AnchorDetailVo> anchorDetail(@Valid @RequestBody AnchorDetailQueryBo bo) {
        log.info("{} anchor/detail, tenantId={}, secUid={}", LOG_PREFIX, bo.getTenantId(), bo.getSecUid());
        return aiAgentReplayService.anchorDetail(bo);
    }

    /**
     * 视频列表（多条件过滤 + 游标分页）。
     *
     * @param bo 查询条件
     * @return 视频列表游标页
     */
    @PostMapping("/video/list")
    @Operation(summary = "视频列表")
    public R<CursorPageVo<VideoItemVo>> videoList(@Valid @RequestBody VideoListQueryBo bo) {
        log.info("{} video/list, tenantId={}, userType={}", LOG_PREFIX, bo.getTenantId(), bo.getUserType());
        return aiAgentReplayService.videoList(bo);
    }

    /**
     * 视频音频段落全文列表。
     *
     * @param bo 查询条件（含 videoId）
     * @return 段落全文列表
     */
    @PostMapping("/video/audio-paragraphs")
    @Operation(summary = "视频音频段落全文")
    public R<List<AudioParagraphVo>> audioParagraphs(@Valid @RequestBody VideoQueryBo bo) {
        log.info("{} video/audio-paragraphs, tenantId={}, videoId={}", LOG_PREFIX, bo.getTenantId(), bo.getVideoId());
        return aiAgentReplayService.audioParagraphs(bo);
    }

    /**
     * 视频场景切片列表。
     *
     * @param bo 查询条件（含 videoId）
     * @return 场景切片列表
     */
    @PostMapping("/video/scene-slices")
    @Operation(summary = "视频场景切片")
    public R<List<SceneSliceVo>> sceneSlices(@Valid @RequestBody VideoQueryBo bo) {
        log.info("{} video/scene-slices, tenantId={}, videoId={}", LOG_PREFIX, bo.getTenantId(), bo.getVideoId());
        return aiAgentReplayService.sceneSlices(bo.getVideoId(), bo.getTenantId());
    }

    /**
     * 视频数据看板（整体汇总 + 分段看盘数据明细）。
     *
     * @param bo 查询条件（含 videoId）
     * @return 数据看板
     */
    @PostMapping("/video/dashboard")
    @Operation(summary = "视频数据看板")
    public R<VideoDashboardVo> videoDashboard(@Valid @RequestBody VideoQueryBo bo) {
        log.info("{} video/dashboard, tenantId={}, videoId={}", LOG_PREFIX, bo.getTenantId(), bo.getVideoId());
        return aiAgentReplayService.videoDashboard(bo);
    }

    /**
     * 视频弹幕列表（游标分页）。
     *
     * @param bo 查询条件（含 videoId）
     * @return 弹幕游标页
     */
    @PostMapping("/video/barrages")
    @Operation(summary = "视频弹幕列表")
    public R<CursorPageVo<BarrageItemVo>> videoBarrages(@Valid @RequestBody BarrageQueryBo bo) {
        log.info("{} video/barrages, tenantId={}, videoId={}", LOG_PREFIX, bo.getTenantId(), bo.getVideoId());
        return aiAgentReplayService.videoBarrages(bo);
    }

    /**
     * 视频在线曲线 + 人群画像 + 投放roi。
     *
     * @param bo 查询条件（含 videoId）
     * @return 在线曲线与画像
     */
    @PostMapping("/video/online-curve")
    @Operation(summary = "视频在线曲线+人群画像+投放roi")
    public R<OnlineCurveVo> onlineCurve(@Valid @RequestBody OnlineCurveQueryBo bo) {
        log.info("{} video/online-curve, tenantId={}, videoId={}", LOG_PREFIX, bo.getTenantId(), bo.getVideoId());
        return aiAgentReplayService.onlineCurve(bo);
    }

    /**
     * 福袋列表（按视频 ID 列表 IN 查询全量，仅租户隔离不做用户级裁剪，内部分批查询）。
     *
     * @param bo 查询条件（含 tenantId + videoIds）
     * @return 福袋信息列表
     */
    @PostMapping("/bless-bag/list")
    @Operation(summary = "福袋列表")
    public R<List<BlessBagListVo>> blessBagList(@Valid @RequestBody BlessBagListQueryBo bo) {
        log.info("{} bless-bag/list, tenantId={}, videoIds.size={}", LOG_PREFIX, bo.getTenantId(), bo.getVideoIds().size());
        return blessBagLogic.listByVideoIds(bo.getTenantId(), bo.getVideoIds());
    }


}
