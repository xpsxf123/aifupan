package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.io.Serializable;

/**
 * 全局提示词 BO
 *
 * @author lj
 * @date 2026-05-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "全局提示词")
public class GlobalProblemBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "ID")
    @NotNull(message = "ID不能为空", groups = {Update.class})
    private Long id;

    @Schema(description = "行业id，0表示全行业")
    private Long tradeId;

    @Schema(description = "提示词类型 0: 运营提示词，1:违规提示词")
    private Integer cueType;

    @Schema(description = "提示词用于：0：单个分析，1：对比分析")
    private Integer applyTo;

    @Schema(description = "提问类型：0：系统提问，1：自己提问")
    private Integer problemType;

    @Schema(description = "实际提示词：具体问题")
    @NotNull(message = "提示词内容不能为空", groups = {Insert.class, Update.class})
    private String problemContent;

    @Schema(description = "描述")
    private String remarks;
}
