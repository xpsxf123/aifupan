package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI分析出来的关键词总数和未匹配上词库的关键词个数列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-21 17:06:53
 */
@Data
@Schema(description = "AI分析出来的关键词总数和未匹配上词库的关键词个数列表项")
public class AiAnalysisRecordListVo extends AiAnalysisRecordVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
