package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 添加主播关键词请求参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2026-03-25
 */
@Data
@Schema(description = "添加主播关键词请求参数")
public class AddAnchorKeywordsBo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主播secUid
     */
    @NotBlank(message = "主播secUid不能为空")
    @Schema(description = "主播secUid")
    private String secUid;

    /**
     * 关键词列表
     */
    @NotEmpty(message = "关键词列表不能为空")
    @Schema(description = "关键词列表")
    private List<String> keywords;

}
