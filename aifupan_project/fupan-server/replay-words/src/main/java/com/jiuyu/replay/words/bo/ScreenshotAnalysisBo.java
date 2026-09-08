package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/3/18 下午7:02
 */
@Data
public class ScreenshotAnalysisBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 数据类型 0视频，1文件，2对比分析
     */
    @Schema(description = "数据类型 0视频，1文件，2对比分析")
    private Integer sourceType;
    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private String sourceId;

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "租户id")
    private Long tenantId;

    @Schema(description = "用户名称-不用传")
    private String nickName;

    @Schema(description = "要分析的id集合")
    private List<Long> ids;

}
