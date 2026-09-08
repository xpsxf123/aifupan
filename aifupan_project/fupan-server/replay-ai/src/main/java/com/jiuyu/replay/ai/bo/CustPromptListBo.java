package com.jiuyu.replay.ai.bo;

import com.jiuyu.replay.generic.bo.common.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户自定义提示词列表查询参数
 *
 * @author jxy
 * @date 2025-01-21
 */
@Data
@Schema(description = "用户自定义提示词列表查询参数")
public class CustPromptListBo extends PageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 提示词标题
     */
    @Schema(description = "提示词标题", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String promptTitle;

    /**
     * 提示词内容
     */
    @Schema(description = "提示词内容", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String promptContent;

    /**
     * 用户id
     */
    @Schema(description = "用户id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long userId;
}

