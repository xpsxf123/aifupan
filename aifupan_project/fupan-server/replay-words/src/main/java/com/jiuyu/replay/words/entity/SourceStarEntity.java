package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 星标表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Data
@TableName("tb_source_star")
public class SourceStarEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 来源id（视频的video_id、文件的file_id、对比的contrast_id）
     */
    private String sourceId;
    /**
     * 来源类型（0：视频 1：文件 2：对比）
     */
    private Integer sourceType;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 租户id
     */
    private Long tenantId;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 修改时间
     */
    private Date updateDate;
    /**
     * 是否已删除
     */
    private Integer isDeleted;

}
