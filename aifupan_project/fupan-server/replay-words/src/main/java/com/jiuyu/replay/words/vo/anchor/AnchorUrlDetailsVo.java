package com.jiuyu.replay.words.vo.anchor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author ：lujie
 * @description：
 * @date ：2025/11/24 16:25
 */
@Data
public class AnchorUrlDetailsVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主播id
     */
    @Schema(description = "主播id")
    private String secUid;

    /**
     * 主播关键词状态 0未获取，1获取成功，2获取失败
     */
    @Schema(description = "主播关键词状态 0未获取，1获取成功，2获取失败")
    private Integer keywordStatus;

    /**
     * 主播关键词
     */
    @Schema(description = "主播关键词")
    private String keyword;

    /**
     * 获取主播关键词失败原因
     */
    @Schema(description = "获取主播关键词失败原因")
    private String keywordError;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateDate;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createDate;

}
