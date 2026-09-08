package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 主播级别知识库
 *
 * @author jy
 * @date 2026-06-29
 */
@Data
@TableName("tb_anchor_knowledge")
public class AnchorKnowledgeEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 主播唯一标识
     */
    private String secUid;
    /**
     * 知识库类型：1=运营知识库，2=敏感词知识库，3=直播间健康值
     */
    private Integer knowledgeType;
    /**
     * 知识库内容
     */
    private String content;
    /**
     * 创建时间
     */
    private Date createDate;
    /**
     * 更新时间
     */
    private Date updateDate;
    /**
     * 是否已删除（0=正常，1=删除）
     */
    private Integer isDeleted;
}
