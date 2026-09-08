package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.oceanEngineData.JuLiangDataListVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "分析结果-云")
public class AnalysisResultCloudVo {

    /**
     * 主播信息
     */
    @Schema(description = "主播信息")
    private AnchorUrlInfoVoUpper anchorInfo;
    /**
     * 视频信息
     */
    @Schema(description = "视频信息")
    private AnchorVideoInfoVoUpper videoInfo;
    /**
     * 文件信息
     */
    @Schema(description = "文件信息")
    private UploadFileInfoVo uploadFile;
    /**
     * 视频/音频播放地址
     */
    @Schema(description = "视频/音频播放地址")
    private String playUrl;
    /**
     * 视频段落词语信息列表
     */
    @Schema(description = "视频段落词语信息列表")
    private List<VideoParagraphAnalysisVoUpper> audioaAlyses;
    /**
     * 文件音频段落词语信息列表
     */
    @Schema(description = "文件音频段落词语信息列表")
    private List<FileParagraphAnalysisVo> fileAudioaAlyses;
    /**
     * 在线人数列表
     */
    @Schema(description = "在线人数列表")
    private List<OnlineNumVoUpper> onlineNumList;
    /**
     * 弹幕数据列表
     */
    @Schema(description = "弹幕数据列表")
    private List<Map<String, Object>> barrageDataList;
    /**
     * 弹幕数量
     */
    @Schema(description = "弹幕数量")
    private Integer totalBarrageNum;
    /**
     * 分析结果的关键词类型信息
     */
    @Schema(description = "分析结果的关键词类型信息")
    private List<AnalysisResultCruxTypeVo> cruxTypeList;
    /**
     * 词语汇总列表
     */
    @Schema(description = "词语汇总列表")
    private List<WordsMarkVo> wordsCollect;
    /**
     * 词语tab列表
     */
    @Schema(description = "词语tab列表")
    private List<WordsTabVo> wordsTabList;

    /**
     * 互动率
     */
    @Schema(description = "互动率")
    private Double interactionPercent;

    /**
     * 销量
     */
    @Schema(description = "销量")
    private Integer purchaseCount;

    /**
     * 销量
     */
    @Schema(description = "销量开始")
    private Integer purchaseCountStart;

    /**
     * 销量
     */
    @Schema(description = "销量结束")
    private Integer purchaseCountEnd;

    /**
     * 总观看人次
     */
    @Schema(description = "总观看人次")
    private Integer totalWatchNum;

    @Schema(description = "数据看板的数据来源")
    private Integer dataSourceType;

    @Schema(description = "uv价值区间范围-起始")
    public Double uvValueStart;

    @Schema(description = "uv价值区间范围-结束")
    public Double uvValueEnd;

    @Schema(description = "销售额-起始")
    public Integer volumeStart;

    @Schema(description = "销售额-结束")
    public Integer volumeEnd;

    /**
     * 巨量数据列表
     */
    @Schema(description = "巨量数据列表")
    private List<JuLiangDataListVo> juLiangDataList;

    /**
     * 投放消耗金额列表（元）
     */
    @Schema(description = "投放消耗金额列表（元）")
    private List<Map<String, Object>> qianchuanCostDataList;

    /**
     * 投放消耗总金额（元）
     */
    @Schema(description = "投放消耗总金额（元）")
    private Integer totalQianchuanCost;

    /**
     * 净成交ROI列表
     */
    @Schema(description = "净成交ROI列表")
    private List<Map<String, Object>> netTransactionRoiDataList;

    /**
     * 净成交ROI
     */
    @Schema(description = "净成交ROI")
    private Double totalNetTransactionRoi;

    /**
     * 视频行业信息
     */
    @Schema(description = "视频行业信息")
    private TradeVo tradeInfo;
}
