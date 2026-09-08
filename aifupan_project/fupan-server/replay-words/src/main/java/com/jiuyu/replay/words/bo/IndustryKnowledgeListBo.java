package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统行业知识库列表查询参数
 *
 * @author jxy
 * @date 2024-06-24
 */
@Data
@Schema(description = "系统行业知识库列表查询参数")
public class IndustryKnowledgeListBo extends PageBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "行业ID")
    private Long tradeId;

    @Schema(description = "知识库类型：1=运营知识库，2=违规知识库，3=敏感词知识库")
    private Integer knowledgeType;

}
