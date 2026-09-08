package com.jiuyu.replay.words.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * 
 * @author liaoxin
 * @date 2025-06-07
 */
@Data
@TableName("tb_analysis_mark")
public class AnalysisMarkEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 视频id
     */
    private String sourceId;

    /**
     * 类型 0视频，1文件
     */
    private Integer sourceType;

    /**
     * 开始段落编号
     */
    private Integer paraphStartNo;

    /**
     * 结束段落编号
     */
    private Integer paraphEndNo;

    /**
     * 标注内容
     */
    private String markContent;

    /**
     * 标注编号
     */
    private Integer markNo;

    /**
     * 开始索引
     */
    private Integer markStartIndex;

    /**
     * 结束索引
     */
    private Integer markEndIndex;

    /**
     * 创建用户id
     */
    private Long createUserId;

    /**
     * 修改用户id
     */
    private Long updateUserId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 是否已删除，1为已删除，0为在用
     */
    private Integer isDeleted;

}