package com.jiuyu.replay.generic.vo.words;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * AI分析关键词与记录关联关系表信息项
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-03-03 18:49:53
 */
@Data
@Schema(description = "AI分析关键词与记录关联关系表信息项")
public class AiAnalysisSensitiveRelaInfoVo extends AiAnalysisSensitiveRelaVo implements Serializable {
	private static final long serialVersionUID = 1L;



}
