package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * VO
 * 
 * @author liaoxin
 * @date 2025-06-07
 */
@Data
@Schema(description = "标注信息")
public class AnalysisMarkVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;
    /**
     * 视频id
     */
    @Schema(description = "视频id")
    private String sourceId;

    /**
     * 文件id
     */
    @Schema(description = "视频类型,类型 0视频，1文件")
    private Integer sourceType;

     /**
     * 开始段落编号
     */
    @Schema(description = "开始段落编号")
    private Integer paraphStartNo;

    /**
     * 结束段落编号
     */
    @Schema(description = "结束段落编号")
    private Integer paraphEndNo;

    /**
     * 标注内容
     */
    @Schema(description = "标注内容")
    private String markContent;

    /**
     * 标注编号
     */
    @Schema(description = "标注编号")
    private Integer markNo;

    /**
     * 开始索引
     */
    @Schema(description = "开始索引")
    private Integer markStartIndex;

    /**
     * 结束索引
     */
    @Schema(description = "结束索引")
    private Integer markEndIndex;

    /**
     * 创建用户id
     */
    @Schema(description = "创建用户id")
    private Long createUserId;

    /**
     * 修改用户id
     */
    @Schema(description = "修改用户id")
    private Long updateUserId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private Date updateTime;

    /**
     * 
     */
    @Schema(description = "是否已删除，0为在用，1为已删除")
    private Integer isDeleted;

}