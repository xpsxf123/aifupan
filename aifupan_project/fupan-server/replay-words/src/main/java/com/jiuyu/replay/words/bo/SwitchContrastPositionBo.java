package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 切换对比定位入参
 */
@Data
@Schema(description = "切换对比定位入参")
public class SwitchContrastPositionBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 对比id
     */
    @Schema(description = "对比id")
    private String contrastId;
}
