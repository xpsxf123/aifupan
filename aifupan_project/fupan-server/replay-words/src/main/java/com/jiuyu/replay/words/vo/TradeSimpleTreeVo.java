package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 行业树形结构简化版（客户端用）
 *
 * @author AI
 * @date 2026-01-05
 */
@Data
@Schema(description = "行业信息（简化版）")
public class TradeSimpleTreeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID")
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

    /**
     * 子行业
     */
    @Schema(description = "子行业")
    private List<TradeSimpleTreeVo> children;
}
