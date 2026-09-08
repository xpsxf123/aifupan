package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 主播级别知识库列表查询参数
 *
 * @author jy
 * @date 2026-06-29
 */
@Data
@Schema(description = "主播级别知识库列表查询参数")
public class AnchorKnowledgeListBo extends PageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID（后端从JWT注入）", hidden = true)
    private Long userId;
}
