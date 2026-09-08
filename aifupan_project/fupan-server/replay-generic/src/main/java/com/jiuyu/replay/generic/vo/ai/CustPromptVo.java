package com.jiuyu.replay.generic.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户自定义提示词 VO
 *
 * @author jxy
 * @date 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户自定义提示词")
public class CustPromptVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
    private Long id;

    /**
     * 提示词标题
     */
    @Schema(description = "提示词标题")
    private String promptTitle;

    /**
     * 提示词内容
     */
    @Schema(description = "提示词内容")
    private String promptContent;

    /**
     * 关联占位符key列表
     */
    @Schema(description = "关联占位符key列表")
    private List<String> placeholderKeys;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer promptSort;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;

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

