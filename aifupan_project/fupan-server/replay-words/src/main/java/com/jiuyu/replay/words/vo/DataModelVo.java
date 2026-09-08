package com.jiuyu.replay.words.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 罗盘数据模型信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Data
@Schema(description = "罗盘数据模型信息")
public class DataModelVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 模型名称
	 */
	@Schema(description = "模型名称")
	private String name;
	/**
	 * 类型 0：通用模型 1：行业模型
	 */
	@Schema(description = "类型 0：通用模型 1：行业模型")
	private Integer type;
	/**
	 * 行业id，通用模型的行业id为0
	 */
	@Schema(description = "行业id，通用模型的行业id为0")
	private Long tradeId;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	@Schema(description = "最后修改时间")
	private Date updateDate;


}
