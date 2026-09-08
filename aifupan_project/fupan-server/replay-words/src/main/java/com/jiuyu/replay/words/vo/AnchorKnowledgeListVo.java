package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 主播级别知识库列表项
 *
 * @author jy
 * @date 2026-06-29
 */
@Data
@Schema(description = "主播级别知识库列表项")
public class AnchorKnowledgeListVo extends AnchorKnowledgeVo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主播名称")
    private String anchorName;
}
