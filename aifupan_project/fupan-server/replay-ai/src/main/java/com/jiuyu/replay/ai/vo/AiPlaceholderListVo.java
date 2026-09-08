package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AI占位符配置 List VO
 *
 * @author jy
 * @date 2026-06-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AI占位符配置列表项")
public class AiPlaceholderListVo extends AiPlaceholderVo implements Serializable {
    private static final long serialVersionUID = 1L;
}
