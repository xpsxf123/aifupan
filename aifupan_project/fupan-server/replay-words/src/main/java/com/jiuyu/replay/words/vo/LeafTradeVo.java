package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 有效的叶子节点行业信息VO
 *
 * @author RayChou
 * @date 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "有效的叶子节点行业信息")
public class LeafTradeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 行业ID
     */
    @Schema(description = "行业ID")
    private Long id;

    /**
     * 行业名称
     */
    @Schema(description = "行业名称")
    private String name;

    /**
     * 父行业ID
     */
    @Schema(description = "父行业ID")
    private Long parentId;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;
}

