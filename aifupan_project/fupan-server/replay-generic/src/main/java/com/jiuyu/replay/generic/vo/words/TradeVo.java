package com.jiuyu.replay.generic.vo.words;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 行业信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
@Data
@Schema(description = "行业信息")
public class TradeVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 行业名称
	 */
	@Schema(description = "行业名称")
	private String name;
	/**
	 * 行业描述
	 */
	@Schema(description = "行业描述")
	private String remarks;
	/**
	 * 默认的通用模型id
	 */
	@Schema(description = "默认的通用模型id")
	private Long defaultGeneralModelId;
	/**
	 * 行业模型id，为0表示没有
	 */
	@Schema(description = "行业模型id，为0表示没有")
	private Long tradeModelId;
	/**
	 * 父行业ID
	 */
	@Schema(description = "父行业ID")
	private Long parentId;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
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
