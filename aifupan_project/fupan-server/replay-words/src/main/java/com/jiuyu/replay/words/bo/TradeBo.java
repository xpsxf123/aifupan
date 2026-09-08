package com.jiuyu.replay.words.bo;

import com.jiuyu.replay.generic.bo.words.CruxTypeScaleBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 行业信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:15:06
 */
@Data
@Schema(description = "行业信息")
public class TradeBo implements Serializable {
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
	 * 关键词类型列表
	 */
	@Schema(description = "关键词类型列表")
	private List<CruxTypeScaleBo> cruxTypeScaleBos;

	/**
	 * 模型名称
	 */
	@Schema(description = "模型名称")
	private String modelName;

	/**
	 * 类型 0：通用模型 1：行业模型
	 */
	@Schema(description = "类型 0：通用模型 1：行业模型")
	private Integer modelType;

}
