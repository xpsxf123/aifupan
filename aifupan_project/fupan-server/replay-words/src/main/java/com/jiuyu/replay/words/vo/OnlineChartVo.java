package com.jiuyu.replay.words.vo;

import com.jiuyu.replay.words.vo.chart.CurveData;
import com.jiuyu.replay.words.vo.chart.CurveDoubleData;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author ：lujie
 * @date ：2025/2/7 下午5:14
 */
@Data
public class OnlineChartVo {

    @Schema(description = "累计观看人数")
    private Integer totalViewersNum;

    @Schema(description = "最大在线人数")
    private Integer maxOnlineNum;

    @Schema(description = "弹幕总数")
    private Integer totalBarrageNum ;

    @Schema(description = "进场人数折线数据 时间戳dateTime，值valueNum")
    private List<CurveData> approachDataList;

    @Schema(description = "离场人数折线数据 时间戳dateTime，值valueNum")
    private List<CurveData> exitPeopleDataList;

    @Schema(description = "在线人数折线数据 时间戳dateTime，值valueNum")
    private List<CurveData> onlineDataList;

    @Schema(description = "弹幕折线数据 时间戳dateTime，值valueNum")
    private List<CurveData> barrageDataList;

    @Schema(description = "福袋信息简略版")
    private List<OnlineChartBlessBagVo> blessBagList;

    @Schema(description = "语数的折线数据 时间戳dateTime，值valueNum")
    private List<CurveData> languageDataList;

    //新增巨量曲线数据

    @Schema(description = "成交折线数据 时间戳dateTime，值valueNum")
    private List<CurveData> payComboCntDataList;

    @Schema(description = "成交金额折线数据 时间戳dateTime，值valueNum")
    private List<CurveDoubleData> payAmtDataList;

    @Schema(description = "新增直播团人数折线数据 时间戳dateTime，值valueNum")
    private List<CurveData> fansClubJoinUcntDataList;

    @Schema(description = "新增粉丝数量折线数据 时间戳dateTime，值valueNum")
    private List<CurveData> followAnchorUcntDataList;

    @Schema(description = "投放消耗折线数据 时间戳dateTime，值valueNum")
    private List<CurveDoubleData> qianchuanCostDataList;

    @Schema(description = "退款金额折线数据 时间戳dateTime，值valueNum")
    private List<CurveDoubleData> refundAmtDataList;

    @Schema(description = "净成交ROI折线数据 时间戳dateTime，值valueNum")
    private List<CurveDoubleData> netTransactionRoiDataList;
}
