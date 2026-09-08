package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 词库列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-10-06 16:34:13
 */
@Data
@Schema(description = "词库列表项")
public class LexiconListVo extends LexiconVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 白名单词语数量
	 */
	@Schema(description = "白名单词语数量")
	private Integer whiteNum = 0;
	/**
	 * 关键词数量
	 */
	@Schema(description = "关键词数量")
	private Integer cruxNum = 0;
	/**
	 * 敏感词数量
	 */
	@Schema(description = "敏感词数量")
	private Integer sensitiveNum = 0;
	/**
	 * 行业名称
	 */
	@Schema(description = "行业名称")
	private String tradeName;


}
