package com.jiuyu.replay.words.vo.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "主播录制信息")
public class AnchorYesterdayRecordVo {

    /**
     * 主播secUid
     */
    @Schema(description = "主播secUid")
    private String secUid;

    /**
     * 昨日录制列表
     */
    @Schema(description = "昨日录制列表")
    private List<AnchorYesterdayRecordItemVo> yesterdayRecordList;
    /**
     * 昨日录制数量
     */
    @Schema(description = "昨日录制数量")
    private Integer yesterdayRecordNum;
    /**
     * 昨日平均场观
     */
    @Schema(description = "昨日平均场观")
    private Integer yesterdayAverageObservationNum;
    /**
     * 昨日平均销售额区间范围-起始
     */
    @Schema(description = "昨日平均销售额区间范围-起始")
    private Integer yesterdayAverageVolumeStart;
    /**
     * 昨日平均销售额区间范围-结束
     */
    @Schema(description = "昨日平均销售额区间范围-结束")
    private Integer yesterdayAverageVolumeEnd;

    /**
     * 前日录制列表
     */
    @Schema(description = "前日录制列表")
    private List<AnchorYesterdayRecordItemVo> dayBeforeRecordList;
    /**
     * 前日录制数量
     */
    @Schema(description = "前日录制数量")
    private Integer dayBeforeRecordNum;
    /**
     * 前日平均场观
     */
    @Schema(description = "前日平均场观")
    private Integer dayBeforeAverageObservationNum;
    /**
     * 前日平均销售额区间范围-起始
     */
    @Schema(description = "前日平均销售额区间范围-起始")
    private Integer dayBeforeAverageVolumeStart;
    /**
     * 前日平均销售额区间范围-结束
     */
    @Schema(description = "前日平均销售额区间范围-结束")
    private Integer dayBeforeAverageVolumeEnd;
}
