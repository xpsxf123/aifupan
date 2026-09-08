package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * ai问答的历史分析段落记录列表查询参数
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Data
@Schema(description = "ai问答的历史分析段落记录列表查询参数")
public class HistoryParagraphListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "资源类型")
	private String sourceType;

	@Schema(description = "资源id")
	private String sourceId;

	@Schema(description = "类型")
	private String type;

}
