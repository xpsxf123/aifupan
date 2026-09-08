package com.jiuyu.replay.words.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 罗盘数据模型信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Data
@Schema(description = "罗盘数据模型信息项")
public class DataModelInfoVo extends DataModelVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 模型的具体项
	 */
	@Schema(description = "模型的具体项")
	private List<DataModelItemVo> modelItemList;
	/**
	 * 是否是默认展示模型 0：否 1：是
	 */
	@Schema(description = "是否是默认展示模型 0：否 1：是")
	private Integer defaultShow;
	/**
	 * 模型名称别名
	 */
	@Schema(description = "模型名称别名")
	private String alias;
}
