package com.jiuyu.replay.ai.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.hibernate.validator.constraints.Length;

import java.io.Serializable;
import java.util.List;

/**
 * 用户自定义提示词 BO
 *
 * @author jxy
 * @date 2025-01-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户自定义提示词")
public class CustPromptBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Schema(description = "ID", requiredMode = Schema.RequiredMode.AUTO)
    @NotNull(message = "ID不能为空", groups = {Update.class})
    private Long id;

    /**
     * 提示词标题
     */
    @Schema(description = "提示词标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "提示词标题不能为空", groups = {Insert.class, Update.class})
    @Length(max = 20, message = "提示词标题不能超过20个字符", groups = {Insert.class, Update.class})
    private String promptTitle;

    /**
     * 提示词内容
     */
    @Schema(description = "提示词内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "提示词内容不能为空", groups = {Insert.class, Update.class})
    private String promptContent;

    /**
     * 关联占位符key列表
     */
    @Schema(description = "关联占位符key列表")
    private List<String> placeholderKeys;

    /**
     * 排序
     */
    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @Max(value = 99, message = "排序值不能超过99", groups = {Insert.class, Update.class})
    @Min(value = 0, message = "排序值不能小于0", groups = {Insert.class, Update.class})
    private Integer promptSort;
}

