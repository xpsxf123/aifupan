package com.jiuyu.replay.words.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 敏感词信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
@Data
@Schema(description = "敏感词信息项")
public class SensitiveWordsInfoVo extends SensitiveWordsVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 相似词列表
	 */
	@Schema(description = "相似词列表")
	private List<SensitiveWordsInfoVo> similarWordList;
	/**
	 * 词语规则列表
	 */
	@Schema(description = "词语规则列表")
	private List<WordRuleInfoVo> ruleList;
	/**
	 * 关键词分类id数组
	 */
	@Schema(description = "关键词分类id数组")
	private List<Long> cruxTypeIdArr;
}
