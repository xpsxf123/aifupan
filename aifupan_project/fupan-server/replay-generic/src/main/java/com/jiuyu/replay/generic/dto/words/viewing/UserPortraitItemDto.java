package com.jiuyu.replay.generic.dto.words.viewing;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "巨量用户画像明细项")
public class UserPortraitItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文本
     */
    @Schema(description = "文本")
    private String label;

    /**
     * 值，1表示100%
     */
    @Schema(description = "值，1表示100%")
    private String value;
}
