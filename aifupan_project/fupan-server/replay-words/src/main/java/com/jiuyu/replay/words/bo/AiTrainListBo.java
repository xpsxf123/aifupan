package com.jiuyu.replay.words.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AI训练列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-20 18:47:39
 */
@Data
@Schema(description = "AI训练列表查询参数")
public class AiTrainListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;
	/**
	 * 状态 0：训练中 1：管理员训练完成 2：超时训练完成
	 */
	@Schema(description = "状态 0：训练中 1：管理员训练完成 2：超时训练完成")
	private String aiStatus;

}
