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
 * 用户自定义提示词表
 *
 * @author jxy
 * @date 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_cust_prompt")
@Schema(description = "用户自定义提示词")
public class CustPromptEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.INPUT)
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
     * 关联占位符key，多个逗号分隔
     */
    @Schema(description = "关联占位符key，多个逗号分隔")
    private String placeholderKeys;

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

    /**
     * 是否已删除
     */
    @Schema(description = "是否已删除")
    private Integer isDeleted;
}

