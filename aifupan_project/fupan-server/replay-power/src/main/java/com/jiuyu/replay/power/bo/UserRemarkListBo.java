package com.jiuyu.replay.power.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 用户备注表列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-02-12 15:59:28
 */
@Data
@Schema(description = "用户备注表列表查询参数")
public class UserRemarkListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

	/**
	 * 用户ID
	 */
	@Schema(description = "模糊搜索查询条件")
	private Long userId;
}
