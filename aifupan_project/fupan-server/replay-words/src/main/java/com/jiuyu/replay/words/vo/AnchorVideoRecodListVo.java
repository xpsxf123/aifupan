package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * @author tisheng
 * @date 2024/9/20
 * @apinNote
 */
@Data
@Schema(description = "分析录制表")
public class AnchorVideoRecodListVo {

    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;

    /**
     * 视频名称
     */
    @Schema(description = "视频名称")
    private String videoName;

    /***
     * 视频时长
     */
    @Schema(description = "视频时长")
    private Long duration;
    /**
     * 视频大小，单位：B
     */
    @Schema(description = "视频大小，单位：B")
    private Long vedioSizie;

    /**
    *行业名称
     */
    @Schema(description ="行业名称")
    private String  tradeName;

    /**
     *开始分析时间
     */
    @Schema(description ="开始分析时间")
    private Date createDate;
}
