package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统行业知识库列表项
 *
 * @author jxy
 * @date 2024-06-24
 */
@Data
@Schema(description = "系统行业知识库列表项")
public class IndustryKnowledgeListVo extends IndustryKnowledgeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "行业名称")
    private String tradeName;

}
