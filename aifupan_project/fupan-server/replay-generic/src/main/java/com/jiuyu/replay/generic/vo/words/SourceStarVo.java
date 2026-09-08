package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 星标信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-01-07
 */
@Data
@Schema(description = "星标信息")
public class SourceStarVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;
    /**
     * 来源id
     */
    @Schema(description = "来源id")
    private String sourceId;
    /**
     * 来源类型（0：视频 1：文件 2：对比）
     */
    @Schema(description = "来源类型（0：视频 1：文件 2：对比）")
    private Integer sourceType;
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
