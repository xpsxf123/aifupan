package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "直播用户来源信息")
public class LiveUserSourceBo implements Serializable {

    /**
     * 百分比
     */
    @Schema(description = "百分比")
    private Double rate;

    /**
     * 行业平均百分比
     */
    @Schema(description = "行业平均百分比")
    private Double avgRate;

    /**
     * 来源名称
     */
    @Schema(description = "来源名称")
    private String title;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 直播间Id
     */
    @Schema(description = "直播间Id")
    private String roomId;
}
