package com.jiuyu.replay.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 全局提示词
 *
 * @author lj
 * @date 2026-05-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_global_problem")
@Schema(description = "全局提示词")
public class GlobalProblemEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "ID")
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
    private String problemContent;

    @Schema(description = "描述")
    private String remarks;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "最后修改时间")
    private Date updateDate;

    @Schema(description = "是否已删除")
    private Integer isDeleted;
}
