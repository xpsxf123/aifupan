package com.jiuyu.replay.words.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 词语匹配规则信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-09 18:16:01
 */
@Data
@Schema(description = "词语匹配规则信息项")
public class WordRuleInfoVo extends WordRuleVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 包含的词语,多个词用下划线#隔开
	 */
	@Schema(description = "包含的词语,多个词用下划线#隔开")
	private String containerWords;
	/**
	 * 包含的词语列表
	 */
	@Schema(description = "包含的词语列表")
	private List<String> containerWordList;
}
