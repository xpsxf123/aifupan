package com.jiuyu.replay.words.vo.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
public class AnchorYesterdayRecordItemVo {

    /**
     * 录制的时间
     */
    @Schema(description = "录制的时间")
    private Date recordDate;
    /**
     * 录制的视频id
     */
    @Schema(description = "录制的视频id")
    private String videoId;
    /**
     * 直播场次号
     */
    @Schema(description = "直播场次号")
    private String batchNumber;
    /**
     * 结束录制时间
     */
    @Schema(description = "结束录制时间")
    private Date endTime ;
    /**
     * 场观人数 -1：未开启数据看板 -2：视频时长不足50分钟
     */
    @Schema(description = "场观人数 -1：未开启数据看板 -2：视频时长不足50分钟")
    private Integer observationNum;
    /**
     * 销售额范围区间-起始，单位：元 -1：未开启数据看板 -2：视频时长不足50分钟
     */
    @Schema(description = "销售额范围区间-起始，单位：元 -1：未开启数据看板 -2：视频时长不足50分钟")
    private Integer volumeStart;
    /**
     * 销售额范围区间-结束，单位：元 -1：未开启数据看板 -2：视频时长不足50分钟
     */
    @Schema(description = "销售额范围区间-结束，单位：元 -1：未开启数据看板 -2：视频时长不足50分钟")
    private Integer volumeEnd;
}
