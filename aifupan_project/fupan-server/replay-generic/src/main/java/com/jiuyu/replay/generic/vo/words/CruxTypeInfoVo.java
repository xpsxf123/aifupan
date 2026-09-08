package com.jiuyu.replay.generic.vo.words;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 关键词类型信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Data
@Schema(description = "关键词类型信息项")
public class CruxTypeInfoVo extends CruxTypeVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 父级id数组
	 */
	@Schema(description = "父级id数组")
	private List<Long> parentIdArr;
	/**
	 * 层级id数组
	 */
	@Schema(description = "层级id数组")
	private List<Long> idArr;
	/**
	 * 父级名称数组
	 */
	@Schema(description = "父级名称数组")
	private List<String> parentNameArr;
	/**
	 * 层级名称数组
	 */
	@Schema(description = "层级名称数组")
	private List<String> nameArr;

}
