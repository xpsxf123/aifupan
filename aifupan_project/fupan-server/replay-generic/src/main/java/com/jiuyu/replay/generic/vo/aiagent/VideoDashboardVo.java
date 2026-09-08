package com.jiuyu.replay.generic.vo.aiagent;

import com.jiuyu.replay.generic.dto.words.viewing.FlowSourceDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 视频数据看板（整体汇总 + 分段看盘数据明细）。
 *
 * <p>汇总源自巨量引擎口径数据；人群画像见在线曲线接口（§4.8），本接口不含画像。</p>
 *
 * @author fupan-server
 */
@Data
public class VideoDashboardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== ① 整体汇总 =====

    private String videoId;
    private String secUid;
    private String batchNumber;
    private String anchorNumber;

    /**
     * 是否带货 0否1是
     */
    private Integer isTakeProduct;

    private Integer totalWatchNum;
    private Integer averageOnlineNum;
    private Integer averageResidenceTime;
    private Integer incrementFollowerCount;
    private Double convertFanRate;
    private Double interactionPercent;
    private Integer volume;
    private Integer purchaseCount;
    private Double customerUnitPrice;
    private Double uvValue;
    private Double goodsConvertRate;
    /**
     * 曝光-观看率
     */
    @Schema(description = "曝光-观看率")
    private Double showWatchCntRatio;

    /**
     * 销售额区间范围-起始(单位:元)
     */
    @Schema(description = "销售额区间范围-起始(单位:元)")
    private Integer volumeStart;
    /**
     * 销售额区间范围-结束(单位:元)
     */
    @Schema(description = "销售额区间范围-结束(单位:元)")
    private Integer volumeEnd;

    /**
     * 销量区间范围-起始
     */
    @Schema(description = "销量区间范围-起始")
    private Integer purchaseCountStart;
    /**
     * 销量区间范围-结束
     */
    @Schema(description = "销量区间范围-结束")
    private Integer purchaseCountEnd;
    /**
     * 客单价区间范围-起始（单位：元）
     */
    @Schema(description = "客单价区间范围-起始（单位：元）")
    private Double customerUnitPriceStart;
    /**
     * 客单价区间范围-结束（单位：元）
     */
    @Schema(description = "客单价区间范围-结束（单位：元）")
    private Double customerUnitPriceEnd;
    /**
     * uv价值区间范围-起始
     */
    @Schema(description = "uv价值区间范围-起始")
    private Double uvValueStart;
    /**
     * uv价值区间范围-结束
     */
    @Schema(description = "uv价值区间范围-结束")
    private Double uvValueEnd;
    /**
     * 带货转换率区间范围-起始
     */
    @Schema(description = "带货转换率区间范围-起始")
    private Double goodsConvertRateStart;
    /**
     * 带货转换率区间范围-结束
     */
    @Schema(description = "带货转换率区间范围-结束")
    private Double goodsConvertRateEnd;

    /**
     * ROI 系列指标
     */
    private Double roi;
    private Double launchRoiAmount;
    private Double refundAmount;
    private Double overallCostRoi;
    private Double netTransactionRoi;

    /**
     * 看播流量结构（来源名+占比）
     */
    private List<FlowSourceDto> watchFlowList;

    /**
     * 成交流量结构
     */
    private List<FlowSourceDto> payFlowList;

    // ===== ② 分段看盘数据明细 =====

    /**
     * 分段（看盘）数据列表；无数据时为空数组
     */
    private List<DashboardParagraphVo> paragraphs;

    // ===== ③ 实时采集补充（来自 onlineChartData；曲线已降采样压缩，非全量）=====
    // 巨量/蝉妈妈看板不含以下时序与福袋，此处由实时采集曲线接口补充并压缩体积。
    // 口径注意：增量型指标（进场/离场/成交/成交额/涨粉/投放消耗/退款）在本看板一律为「累计到该时点」，
    // 不是分钟增量——看板只保留约 5 个点，抽样后的单分钟增量无从解读；累计值任取一点都成立。
    // 在线人数是瞬时水平量，不做累计。全量分钟增量曲线请走 4.8 在线曲线接口。

    /**
     * 弹幕总数（实时采集）
     */
    private Integer totalBarrageNum;

    /**
     * 在线人数折线（瞬时值，降采样）
     */
    private List<CurvePointVo> onlineDataList;

    /**
     * 进场人数折线（累计值，降采样）
     */
    private List<CurvePointVo> approachDataList;

    /**
     * 离场人数折线（累计值，降采样）
     */
    private List<CurvePointVo> exitPeopleDataList;

    /**
     * 成交折线（巨量百应时序，累计值，降采样）
     */
    private List<CurvePointVo> payComboCntDataList;

    /**
     * 成交金额折线（巨量百应时序，累计值，降采样）
     */
    private List<CurvePointVo> payAmtDataList;

    /**
     * 新增粉丝折线（巨量百应时序，累计值，降采样）
     */
    private List<CurvePointVo> followAnchorUcntDataList;

    /**
     * 投放消耗折线（巨量百应时序，累计值，降采样）
     */
    private List<CurvePointDoubleVo> qianchuanCostDataList;

    /**
     * 退款金额折线（巨量百应时序，累计值，降采样）
     */
    private List<CurvePointDoubleVo> refundAmtDataList;

    /**
     * 净成交ROI折线（累计口径：(累计成交额 − 累计退款) / 累计投放消耗，降采样）
     */
    private List<CurvePointDoubleVo> netTransactionRoiDataList;

    /**
     * 福袋（压缩版：时点+奖品+参与人数）
     */
    private List<DashboardBlessBagVo> blessBagList;

    // ===== ④ 截图识别兜底（无巨量/蝉妈妈看板时填充）=====

    /**
     * 数据截图识别内容（仅当无看板数据时取 tb_data_screenshot 已识别完成的截图；否则为空数组）
     */
    private List<DashboardScreenshotVo> screenshots;
}
