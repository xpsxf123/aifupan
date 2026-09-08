package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * ai问答的历史分析段落记录信息项
 *
 * @author lj
 * @email 
 * @date 2025-03-22 15:36:36
 */
@Data
@Schema(description = "ai问答的历史分析段落记录信息项")
public class HistoryParagraphInfoVo extends HistoryParagraphVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
