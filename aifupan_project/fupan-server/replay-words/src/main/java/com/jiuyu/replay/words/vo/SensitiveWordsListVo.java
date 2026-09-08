package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 敏感词列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
@Data
@Schema(description = "敏感词列表项")
public class SensitiveWordsListVo extends SensitiveWordsVo implements Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * 行业名称
	 */
	@Schema(description = "行业名称")
	private String tradeName;
	/**
	 * 相似词列表
	 */
	@Schema(description = "相似词列表")
	private List<SensitiveWordsListVo> similarWordList;
	/**
	 * 相似词是否标注（只有相似词有）
	 */
	@Schema(description = "相似词是否标注（只有相似词有）")
	private Integer isMark;

	/**
	 * 关键词分类名称
	 */
	@Schema(description = "关键词分类名称")
	private String cruxTypeName;

}
