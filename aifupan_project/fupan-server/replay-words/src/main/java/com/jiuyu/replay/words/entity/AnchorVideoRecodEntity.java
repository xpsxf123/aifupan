package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tisheng
 * @date 2024/9/11
 * @apinNote
 */
@Data
@TableName("tb_anchor_video_recod")
@Schema(description = "分析记录")
public class AnchorVideoRecodEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视频id
     */
    @Schema(description = "id")
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    @Schema(description = "userid")
    private Long userId;
    @Schema(description = "上传视频或录制视频时上传的id 必须上传")
    private String videoId;
    @Schema(description = "视频名称")
    private String videoName;
    @Schema(description = "开始时间")
    private Date startTime;

    @Schema(description = "视频时长")
    private Long duration;

    @Schema(description = "结束时间")
    private Date endTime ;

    @Schema(description = " 主播url表的唯一标识")
    private String  secUid;

    @Schema(description ="行业id")
    private Long  tradeId;
    /**
     * 平台类型 0：全平台 1：抖音 2：快手 3：视频号
     */
    @Schema(description ="平台类型 0：全平台 1：抖音 2：快手 3：视频号")
    private String  platformType;
    // @Schema(description ="类型 1 代表已经录制未上传分享  2 上传分享")
    // private Integer type;
    /**
     * 创建时间
     */
    @Schema(description ="创建时间")
    private Date createDate;
    /**
     * 逻辑删除 0  1删除
     */
    @Schema(description ="逻辑删除")
    private Integer isDeleted;



}
