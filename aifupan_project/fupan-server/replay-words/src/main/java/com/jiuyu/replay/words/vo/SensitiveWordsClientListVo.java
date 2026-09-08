package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 客户端自定义词语列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
@Data
@Schema(description = "客户端自定义词语列表项")
public class SensitiveWordsClientListVo extends SensitiveWordsClientVo implements Serializable {
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
	private List<SensitiveWordsClientListVo> similarWordList;
	/**
	 * 相似词是否标注（只有相似词有）
	 */
	@Schema(description = "相似词是否标注（只有相似词有）")
	private Integer isMark;
	/**
	 * 用户名称
	 */
	@Schema(description = "用户名称")
	private String userNickName;
	/**
	 * 用户手机号
	 */
	@Schema(description = "用户手机号")
	private String userPhone;

}
