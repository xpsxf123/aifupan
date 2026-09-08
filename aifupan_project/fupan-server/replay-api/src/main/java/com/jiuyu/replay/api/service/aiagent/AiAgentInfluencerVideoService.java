package com.jiuyu.replay.api.service.aiagent;

import com.jiuyu.replay.generic.bo.aiagent.HotSearchAnalyticsOverviewBo;
import com.jiuyu.replay.generic.bo.aiagent.HotSearchKeywordQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.HotSearchVideoQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerAnalyticsOverviewBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerDetailBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerListQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerSearchBo;
import com.jiuyu.replay.generic.bo.aiagent.InfluencerVideoStatsBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoAudioTextBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoDetailBatchBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoGlobalSearchBo;
import com.jiuyu.replay.generic.vo.aiagent.CursorPageVo;
import com.jiuyu.replay.generic.vo.aiagent.HotSearchAnalyticsVo;
import com.jiuyu.replay.generic.vo.aiagent.HotSearchKeywordVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerAnalyticsVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerDetailBatchVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerItemVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoItemVo;
import com.jiuyu.replay.generic.vo.aiagent.InfluencerVideoStatsVo;
import com.jiuyu.replay.generic.vo.aiagent.StreamPageVo;
import com.jiuyu.replay.generic.vo.aiagent.VideoAudioTextVo;
import com.jiuyu.replay.generic.vo.common.R;

import java.util.List;

/**
 * AI Agent 达人短视频开放接口服务。
 *
 * <p>达人范围取租户达人订阅，短视频范围取租户已采集视频，均按 tenantId（子账号叠加 userId）隔离。</p>
 *
 * @author fupan-server
 */
public interface AiAgentInfluencerVideoService {

    /**
     * 达人列表（租户达人订阅维度，附短视频数量 + 最近采集时间，游标分页）。
     *
     * @param bo 查询条件
     * @return 达人列表游标页
     */
    R<CursorPageVo<InfluencerItemVo>> influencerList(InfluencerListQueryBo bo);

    /**
     * 批量获取短视频原文音频转文字内容（按 tb_video_info id 直查）。
     *
     * @param bo 查询条件
     * @return 原文内容列表
     */
    R<List<VideoAudioTextVo>> batchVideoAudioText(VideoAudioTextBatchBo bo);

    /**
     * 批量达人短视频统计（仅租户已采集：数量 + 点赞/评论/分享/收藏汇总）。
     *
     * @param bo 查询条件
     * @return 统计列表（与入参达人 id 一一对应）
     */
    R<List<InfluencerVideoStatsVo>> batchInfluencerVideoStats(InfluencerVideoStatsBatchBo bo);

    /**
     * 短视频检索（按达人/关键词/标签/指标过滤，keyset 游标流式分页）。
     *
     * <p>{@code collectedOnly=false} 读公共爬取全库；{@code collectedOnly=true} 仅限本租户已采集范围
     * （JOIN tb_video_user_video 在库内过滤，其余参数与全库一致）。</p>
     *
     * @param bo 查询条件
     * @return 短视频页
     */
    R<StreamPageVo<InfluencerVideoItemVo>> globalVideoSearch(VideoGlobalSearchBo bo);

    /**
     * 批量短视频完整明细（按 tb_video_info id 直查，可选加载原文音频转文字）。
     *
     * @param bo 查询条件
     * @return 明细列表（与入参 id 顺序一致）
     */
    R<List<InfluencerVideoItemVo>> videoDetailBatch(VideoDetailBatchBo bo);

    /**
     * 全库达人搜索（跨租户读公共爬取库，按昵称/账号/平台/粉丝过滤，keyset 游标流式分页）。
     *
     * @param bo 查询条件
     * @return 达人页
     */
    R<StreamPageVo<InfluencerItemVo>> influencerSearch(InfluencerSearchBo bo);

    /**
     * 爆款关键词列表（全库，keyset 游标流式分页）。
     *
     * @param bo 查询条件
     * @return 爆款关键词页
     */
    R<StreamPageVo<HotSearchKeywordVo>> hotSearchKeywordList(HotSearchKeywordQueryBo bo);

    /**
     * 爆款视频列表（某爆款关键词下的短视频，按关键词/标签/指标过滤，keyset 游标流式分页）。
     *
     * @param bo 查询条件
     * @return 短视频页
     */
    R<StreamPageVo<InfluencerVideoItemVo>> hotSearchVideoList(HotSearchVideoQueryBo bo);

    /**
     * 达人统计概览（单达人档案下钻，服务端一次聚合，见设计 doc17 §6.20）。
     *
     * <p>按 scope 取本租户已采集 / 全库爬取作品，在时间窗（默认近 3 个月、跨度硬上限 1 年）内一次算好
     * KPI / 互动结构 / 时长分布 / 发布热力 / 互动趋势 / 爆款 / 能力雷达，前端仅渲染，禁止拉全量自算。</p>
     *
     * @param bo 查询条件
     * @return 统计概览
     */
    R<InfluencerAnalyticsVo> influencerAnalyticsOverview(InfluencerAnalyticsOverviewBo bo);

    /**
     * 爆款选题拆解聚合（某爆款关键词下的选题分析，服务端一次聚合，见接口文档 §6.21）。
     *
     * <p>在时间窗（默认近 7 天、跨度硬上限 1 年）内取该 searchId 互动量 Top N 作品，一次算好门槛与集中度 /
     * 互动结构 / 趋势 / 时长与发布节奏 / 头部达人 / 高频 #标签 / 本租户文案可用条数，前端仅渲染。</p>
     *
     * @param bo 查询条件
     * @return 选题拆解聚合结果
     */
    R<HotSearchAnalyticsVo> hotSearchAnalyticsOverview(HotSearchAnalyticsOverviewBo bo);

    /**
     * 批量达人档案（按 author_id 直查公共爬取达人库，不分页，见接口文档 §6.22）。
     *
     * <p>命中率天然低于 100%：爆款同步链路只把达人信息冗余写进 tb_video_hot_search_video，
     * 不落 tb_video_influencer_info，故爆款上榜账号未必有档案。未命中的 id 原样回显在
     * {@code notFoundAuthorIds}，由调用方决定降级展示。</p>
     *
     * @param bo 查询条件
     * @return 达人档案 + 未命中 id
     */
    R<InfluencerDetailBatchVo> influencerDetailBatch(InfluencerDetailBatchBo bo);
}
