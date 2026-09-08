package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ：lujie
 * @description：视频对应的ai配置选项
 * @date ：2025/3/27 下午5:30
 */
@Data
@Schema(description = "视频对应的ai配置选项")
public class AiOptionConfigVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否有数据截图 0没有，1有")
    private Integer hasDataScreenshot;

    @Schema(description = "是否有数据看版 0没有，1有")
    private Integer hasBoard;
}
