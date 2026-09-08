package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统行业知识库
 *
 * @author jxy
 * @date 2024-06-24
 */
@Data
@TableName("tb_industry_knowledge")
public class IndustryKnowledgeEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    /**
     * 行业ID
     */
    private Long tradeId;
    /**
     * 知识库类型：1=运营知识库，2=违规知识库，3=敏感词知识库
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
