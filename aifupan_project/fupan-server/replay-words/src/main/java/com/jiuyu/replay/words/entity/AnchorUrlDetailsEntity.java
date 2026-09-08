package com.jiuyu.replay.words.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 主播的附加表
 *
 * @author lj
 * @date 2025-11-22
 */
@Data
@TableName("tb_anchor_url_details")
public class AnchorUrlDetailsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主播id
     */
    @TableId(value = "sec_uid")
    private String secUid;

    /**
     * 主播关键词状态 0未获取，1获取成功，2获取失败
     */
    private Integer keywordStatus;

    /**
     * 主播关键词
     */
    private String keyword;

    /**
     * 获取主播关键词失败原因
     */
    private String keywordError;

    /**
     * 更新时间
     */
    private Date updateDate;

    /**
     * 创建时间
     */
    private Date createDate;

}

