package com.jiuyu.replay.words.vo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@Data
@Schema(description = "AI分析出来的关键词总数和未匹配上词库的关键词个数信息项")
public class AiAnalysisRecordInfoVo extends AiAnalysisRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
