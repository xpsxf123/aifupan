package com.jiuyu.replay.words.bo.viewing;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class LivePortraitGenderBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 百分比
     */
    @Schema(description = "百分比")
    private Double rate;
    /**
     * 性别
     */
    @Schema(description = "性别")
    private String title;
    /**
     * 直播间Id
     */
    @Schema(description = "直播间Id")
    private String roomId;

}
