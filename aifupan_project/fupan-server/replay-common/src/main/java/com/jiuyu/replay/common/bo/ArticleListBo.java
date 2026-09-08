package com.jiuyu.replay.common.bo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文章列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-08-23 20:39:45
 */
@Data
@Schema(description = "文章列表查询参数")
public class ArticleListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;
	/**
	 * 按文章类型筛选
	 */
	@Schema(description = "按文章类型筛选")
	private Integer type;

}
