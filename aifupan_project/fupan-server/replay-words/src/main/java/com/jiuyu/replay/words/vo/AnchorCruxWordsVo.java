package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 主播关键词VO
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-14
 */
@Data
@Schema(description = "主播关键词信息")
public class AnchorCruxWordsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 关键词ID
     */
    @Schema(description = "关键词ID")
    private Long id;
    /**
     * 主播secUid
     */
    @Schema(description = "主播secUid")
    private String secUid;
    /**
     * 关键词
     */
    @Schema(description = "关键词")
    private String keyword;
    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;
    /**
     * 租户id
     */
    @Schema(description = "租户id")
    private Long tenantId;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;
    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private Date updateDate;

}
