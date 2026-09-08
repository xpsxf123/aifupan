package com.jiuyu.replay.power.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户详情表列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-09-14 10:12:41
 */
@Data
@Schema(description = "用户详情表列表查询参数")
public class UserDetailsListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
