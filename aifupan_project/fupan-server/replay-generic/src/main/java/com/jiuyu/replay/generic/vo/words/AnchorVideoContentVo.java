package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/5/26 下午8:27
 */
@Schema(description = "视频内容出参")
@Data
public class AnchorVideoContentVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "视频id")
    private String videoId;

    @Schema(description = "类型，1是自然原文，2是优化原文")
    private Integer type;

//    /**
//     * 自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败
//     */
//    @Schema(description = "自然原文生成状态 0待生成，1生成中，2生成成功，3生成失败")
//    private Integer natureContentStatus;
//    /**
//     * 优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败
//     */
//    @Schema(description = "优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败")
//    private Integer optimizeContentStatus;


    /**
     * 优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败
     */
    @Schema(description = "优化原文生成状态 0待生成，1生成中，2生成成功，3生成失败")
    private Integer contentStatus;

    /**
     * 视频信息
     */
    @Schema(description = "视频信息")
    private AnchorVideoInfoVo anchorVideo;

    /**
     * 视频内容
     */
    @Schema(description = "视频内容")
    private List<VideoContentVo> videoContentList;
}
