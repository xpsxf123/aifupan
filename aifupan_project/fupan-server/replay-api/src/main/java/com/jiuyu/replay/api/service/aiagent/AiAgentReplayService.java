package com.jiuyu.replay.api.service.aiagent;

import com.jiuyu.replay.generic.bo.aiagent.AiAgentBaseBo;
import com.jiuyu.replay.generic.bo.aiagent.AnchorDetailQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.AnchorListQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.BarrageQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.OnlineCurveQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.SensitiveWordsQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.TradeOptionsQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.TradeParentsQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoListQueryBo;
import com.jiuyu.replay.generic.bo.aiagent.VideoQueryBo;
import com.jiuyu.replay.generic.vo.aiagent.*;
import com.jiuyu.replay.generic.vo.common.R;
import com.jiuyu.replay.words.vo.SocketCollectMessageVo;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * AI Agent 复盘数据开放接口服务（只读查询）。
 *
 * <p>身份从请求体显式取（userId/tenantId/userType），不依赖 JWT；据此做租户隔离 + 数据范围裁剪。</p>
 *
 * @author fupan-server
 */
public interface AiAgentReplayService {

    /**
     * 主播列表（多条件过滤 + 游标分页）。
     *
     * @param bo 查询条件
     * @return 主播列表游标页
     */
    R<CursorPageVo<AnchorItemVo>> anchorList(AnchorListQueryBo bo);

    /**
     * 当前范围内所有主播所属行业的「ID + 名称」去重列表。
     *
     * @param bo 身份
     * @return 行业聚合列表
     */
    R<List<TradeOptionVo>> tradeOptions(TradeOptionsQueryBo bo);

    /**
     * 行业层级链（自身 + 各级父行业），按层级排序，离 tradeId 越近越靠前。
     *
     * @param bo 查询条件（含 tradeId）
     * @return 行业层级链（id + name + parentId）
     */
    R<List<TradeParentVo>> tradeParents(TradeParentsQueryBo bo);

    /**
     * 行业敏感词库（按行业父链取词：4 级自身 + 3/2/1 级父行业 + 全行业，系统词，已去重并按严重度排序）。
     *
     * <p>面向 AI Agent 输出话术前的合规自检。取词为父链向上回溯叠加全行业(id=1)，平台按 {@code platform_type IN (0, platform+1)} 收敛。</p>
     *
     * @param bo 查询条件（含 tradeId、可选 platform / wordsType）
     * @return 敏感词条目列表（按 level 升序，最严重在前）
     */
    R<List<SensitiveWordVo>> sensitiveWords(SensitiveWordsQueryBo bo);

    /**
     * 主播详情（基础信息 + 配置信息 + 基础设置，字典已解析）。
     *
     * @param bo 查询条件（含 secUid）
     * @return 主播详情
     */
    R<AnchorDetailVo> anchorDetail(AnchorDetailQueryBo bo);

    /**
     * 视频列表（多条件过滤 + 游标分页）。
     *
     * @param bo 查询条件
     * @return 视频列表游标页
     */
    R<CursorPageVo<VideoItemVo>> videoList(VideoListQueryBo bo);

    /**
     * 视频音频段落全文列表（仅段落序号 + 全文文本）。
     *
     * @param bo 查询条件（含 videoId）
     * @return 段落全文列表
     */
    R<List<AudioParagraphVo>> audioParagraphs(VideoQueryBo bo);

    /**
     * 获取视频场景片段列表。
     *
     * @param videoId 视频ID
     * @param tenantId 租户ID
     * @return 场景片段列表
     */
    R<List<SceneSliceVo>> sceneSlices(String videoId, long tenantId);

    /**
     * 视频数据看板（整体汇总 + 分段看盘数据明细）。
     *
     * @param bo 查询条件（含 videoId）
     * @return 数据看板
     */
    R<VideoDashboardVo> videoDashboard(VideoQueryBo bo);

    /**
     * 视频弹幕列表（游标分页）。
     *
     * @param bo 查询条件（含 videoId）
     * @return 弹幕游标页
     */
    R<CursorPageVo<BarrageItemVo>> videoBarrages(BarrageQueryBo bo);

    /**
     * 视频在线曲线 + 人群画像。
     *
     * @param bo 查询条件（含 videoId）
     * @return 在线曲线与画像
     */
    R<OnlineCurveVo> onlineCurve(OnlineCurveQueryBo bo);





    /**
     * 获取视频在线曲线关联的采集统计数据（累计场观/场观/弹幕总数/最高在线）。
     *
     * @param videoIds 视频ID列表
     * @return 视频ID与采集统计数据映射，能查出即代表已生成在线曲线数据
     */
    Map<String, SocketCollectMessageVo> getVideoHasChartDataMap(Collection<String> videoIds);


}
