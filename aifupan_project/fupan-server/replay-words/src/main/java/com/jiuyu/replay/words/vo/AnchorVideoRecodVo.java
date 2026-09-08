package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author tisheng
 * @date 2024/9/11
 * @apinNote
 */
@Data
@Schema(description = "分析记录")
public class AnchorVideoRecodVo  implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 行业名称
     */
    @Schema(description = "行业名称")
    private String tradeName;

    /**
     * 主播名称
     */
    @Schema(description = "主播名称")
    private String anchorName;

    /**
     * 上传分析内容
     */
    @Schema(description = "上传分析内容")
    List<AudioAnalysisVo> udioAnalysisVo;


    /**
     * userid
     */
    @Schema(description = "userid")
    private Long userId;

    /**
     * 上传视频或录制视频时上传的id
     */
    @Schema(description = "上传视频或录制视频时上传的id")
    private String videoId;

    /**
     * 用户昵称
     */
    @Schema(description = "用户昵称")
    private String userName;


    /**
     * 视频名称
     */
    @Schema(description = "视频名称")
    private String videoName;

    /**
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
     *平台类型
     */
    @Schema(description = "平台类型")
    private String platformType;


    /**
     *平台类型
     */
    @Schema(description = "分析时间")
    private Date createDate;
    /**
     * 分析状态
     */
    @Schema(description = "分析状态")
    private Integer analysisStatus;


}
