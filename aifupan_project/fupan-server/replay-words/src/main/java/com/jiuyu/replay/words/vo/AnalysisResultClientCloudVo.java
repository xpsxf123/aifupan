package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.generic.vo.words.*;
import com.jiuyu.replay.words.vo.file.UploadFileInfoVo;
import com.jiuyu.replay.words.vo.oceanEngineData.JuLiangDataListVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分析结果-云-客户端")
public class AnalysisResultClientCloudVo {

    /**
     * 主播信息
     */
    @Schema(description = "主播信息")
    private AnchorUrlInfoVo anchorInfo;
    /**
     * 视频信息
     */
    @Schema(description = "视频信息")
    private AnchorVideoInfoVo videoInfo;
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
     * 段落词语信息列表
     */
    @Schema(description = "段落词语信息列表")
    private List<OnlineAnalysisItemVo> analysisList;
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
     * 在线人数列表
     */
    @Schema(description = "在线人数列表")
    private List<OnlineNumInfoVo> onlineNumList;
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
     * 视频行业信息
     */
    @Schema(description = "视频行业信息")
    private TradeVo tradeInfo;
}
