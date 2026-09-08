package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(name = "直播省份信息")
public class LivePortraitProvinceBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 百分比
     */
    @Schema(description = "百分比")
    private Double rate;


    /**
     * 省份名称
     */
    @Schema(description = "省份名称")
    private String title;

    /**
     * 直播间Id
     */
    @Schema(description = "直播间Id")
    private String roomId;
}
