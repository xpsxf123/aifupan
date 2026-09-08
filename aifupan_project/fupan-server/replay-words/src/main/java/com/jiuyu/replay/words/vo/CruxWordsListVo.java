package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 关键词列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
@Data
@Schema(description = "关键词列表项")
public class CruxWordsListVo extends CruxWordsVo implements Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * 行业名称
	 */
	@Schema(description = "行业名称")
	private String tradeName;

}
