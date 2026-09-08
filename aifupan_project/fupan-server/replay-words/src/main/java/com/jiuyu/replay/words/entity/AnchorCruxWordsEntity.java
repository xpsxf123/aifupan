package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 主播关键词表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
@Data
@TableName("tb_anchor_crux_words")
public class AnchorCruxWordsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 关键词
     */
    private String keyword;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 修改时间
     */
    private Date updateDate;
    /**
     * 是否已删除 0：否 1：是
     */
    private Integer isDeleted;

}
