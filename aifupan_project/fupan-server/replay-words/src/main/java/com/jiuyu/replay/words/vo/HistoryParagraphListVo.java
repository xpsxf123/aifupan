package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ai问答的历史分析段落记录列表项
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Data
@Schema(description = "ai问答的历史分析段落记录列表项")
public class HistoryParagraphListVo extends HistoryParagraphVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
