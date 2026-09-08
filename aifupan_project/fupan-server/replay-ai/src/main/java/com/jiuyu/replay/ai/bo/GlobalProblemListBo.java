package com.jiuyu.replay.ai.bo;

import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 全局提示词列表查询参数
 *
 * @author lj
 * @date 2026-05-21
 */
@Data
@Schema(description = "全局提示词列表查询参数")
public class GlobalProblemListBo extends PageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "行业id")
    private Long tradeId;

    @Schema(description = "提示词类型 0: 运营提示词，1:违规提示词")
    private Integer cueType;

    @Schema(description = "提示词用于：0：单个分析，1：对比分析")
    private Integer applyTo;

    @Schema(description = "提问类型：0：系统提问，1：自己提问")
    private Integer problemType;
}
