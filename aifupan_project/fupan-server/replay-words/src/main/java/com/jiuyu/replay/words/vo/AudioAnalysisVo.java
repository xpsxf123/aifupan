package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "视频分析内容")
public class AudioAnalysisVo  implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视频videoId
     */
    @Schema(description ="视频videoId")
    private String videoId;
    /**
     * 用户id
     */
    @Schema(description ="用户id")
    private Long userId;
    /**
     * 识别状态 0：成功 1：失败
     */
    @Schema(description ="识别状态 0：成功 1：失败")
    private Integer status;
    /**
     * 分析json数据
     */
    @Schema(description ="分析json数据")
    private String dataJson;
    /**
     * 行业id
     */
    @Schema(description ="行业Id")
    private String tradeId;
    /**
     * 第几段
     */
    @Schema(description ="第几段")
    private Integer paragraph;
    /**
     * 版本号，如果一个行业存在多个分析结果，取最高版本号的
     */
    @Schema(description ="版本号，如果一个行业存在多个分析结果，取最高版本号的")
    private Integer version;
}
