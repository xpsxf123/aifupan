package com.jiuyu.replay.common.bo;


import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import com.jiuyu.replay.common.bo.PageBo;

/**
 * 系统配置列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2025-04-24 18:55:01
 */
@Data
@Schema(description = "系统配置列表查询参数")
public class SystemConfigListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

}
