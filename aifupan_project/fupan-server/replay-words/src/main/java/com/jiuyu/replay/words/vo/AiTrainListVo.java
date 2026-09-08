package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AI训练列表项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
@Data
@Schema(description = "AI训练列表项")
public class AiTrainListVo extends AiTrainVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用户名称
	 */
	@Schema(description = "用户名称")
	private String userNickName;
	/**
	 * 视频名称
	 */
	@Schema(description = "视频名称")
	private String videoName;
	/**
	 * 行业名称
	 */
	@Schema(description = "行业名称")
	private String tradeName;

}
