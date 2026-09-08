package com.jiuyu.replay.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * AI占位符配置表
 *
 * @author jy
 * @date 2026-06-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_ai_placeholder")
@Schema(description = "AI占位符配置")
public class AiPlaceholderEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "ID")
    private Long id;

    @TableField("placeholder_name")
    @Schema(description = "占位符名称")
    private String name;

    @Schema(description = "缩写（前端展示）")
    private String abbreviation;

    @Schema(description = "占位符key")
    private String placeholderKey;

    @Schema(description = "参数说明")
    private String paramDesc;

    @TableField("remark")
    @Schema(description = "功能说明")
    private String description;

    @Schema(description = "细节说明")
    private String detail;

    @Schema(description = "分类：1-默认展示，2-更多参数")
    private Integer category;

    @Schema(description = "是否前端展示 0否 1是")
    private Integer isFrontendShow;

    @Schema(description = "是否默认勾选 0否 1是")
    private Integer isDefaultChecked;

    @TableField("status_flag")
    @Schema(description = "状态 0启用 1禁用")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "更新时间")
    private Date updateDate;

    @Schema(description = "是否已删除")
    private Integer isDeleted;
}
