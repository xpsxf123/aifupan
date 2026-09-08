package com.jiuyu.replay.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 全局提示词列表 VO
 *
 * @author lj
 * @date 2026-05-21
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "全局提示词列表")
public class GlobalProblemListVo extends GlobalProblemVo {
    private static final long serialVersionUID = 1L;

    @Schema(description = "行业名称")
    private String tradeName;
}
