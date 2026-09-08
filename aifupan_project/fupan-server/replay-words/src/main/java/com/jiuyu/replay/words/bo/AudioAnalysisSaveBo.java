package com.jiuyu.replay.words.bo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class AudioAnalysisSaveBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description ="ID")
    private Long id;
    /**
     * 视频唯一标识
     */
    @Schema(description ="视频唯一标识")
    private String videoId;
    /**
     * 用户id
     */
    @Schema(description ="用户id")
    private Long userId;
    /**
     * 识别状态  0：成功 1：失败
     */
    @Schema(description ="识别状态  0：成功 1：失败")
    private Integer status;
    /**
     * 分析Json数据
     */
    @Schema(description ="分析Json数据")
    private String dataJson;
    /**
     * 行业id
     */
    @Schema(description ="行业Id")
    private String tradeId;
    /**
     * 当前是第几段，从1开始
     */
    @Schema(description ="当前是第几段，从1开始")
    private Integer paragraph;
    /**
     * 创建时间
     */
    @Schema(description ="创建时间")
    private Date createDate;
    /**
     * 是否已删除
     */
    @Schema(description ="是否已删除")
    private Integer isDeleted;
    /**
     * 版本号
     */
    @Schema(description ="版本号")
    private Integer version;
}
