package com.jiuyu.replay.system.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 字典列表查询参数
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-07-08 11:46:20
 */
@Data
@ToString(callSuper = true)
@Schema(description = "字典列表查询参数")
public class DictDataListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;
	/**
	 * 字典类型Id
	 */
	@Schema(description = "字典类型Id")
	private Long typeId;
	/**
	 * 字典类型标识
	 */
	@Schema(description = "字典类型标识")
	private String typeLogo;
	/**
	 * 字典label
	 */
	@Schema(description = "字典label")
	private String label;
	/**
	 * 字典值
	 */
	@Schema(description = "字典值")
	private String value;
	/**
	 * 是否查所有(包括禁用的)  0:否 1：是
	 */
	@Schema(description = "是否查所有(包括禁用的) 0:否 1：是")
	private Integer isAll;

}
