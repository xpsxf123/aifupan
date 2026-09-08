package com.jiuyu.replay.generic.vo.power;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 销售级联选择器 VO
 *
 * @author jxy
 * @date 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "销售级联选择器")
public class SalesCascaderVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 值（销售ID或销售类型）
     */
    @Schema(description = "值（销售ID或销售类型）")
    private Object value;

    /**
     * 标签（显示文本）
     */
    @Schema(description = "标签（显示文本）")
    private String label;

    /**
     * 子节点
     */
    @Schema(description = "子节点")
    private List<SalesCascaderVo> children;
}

