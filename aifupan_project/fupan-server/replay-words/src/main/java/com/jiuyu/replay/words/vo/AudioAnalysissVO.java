package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tisheng
 * @date 2024/9/15
 * @apinNote
 */
@Data
@Schema(description = "视频分析客户端需要dVO")
public class AudioAnalysissVO implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * 视频id
     */
    @Schema(description ="tb_anchor_video表的Id")
    private String videoId;

    /**
     * 用户id
     */
    @Schema(description ="分享人id")
    private Long userId;

    /**
     * 识别状态  0：成功 1：失败
     */
    @Schema(description ="识别状态  0：成功 1：失败")
    private Integer status;

    /**
     * 富文本内容
     */
    @Schema(description ="文本内容")
    private String dataJson;

    @Schema(description ="行业Id")
    private String tradeId;

    @Schema(description ="第几段")
    private Integer paragraph;

    @Schema(description ="第几次")
    private Integer num;

    @Schema(description ="创建时间")
    private Date createDate;

    /**
     * 逻辑删除 0  1删除
     */
    @Schema(description ="逻辑删除")
    private Integer isDeleted;


    /**
     * 版本号
     */
    @Schema(description ="版本号")
    private Integer version;
}
