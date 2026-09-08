package com.jiuyu.replay.generic.vo.words;


import com.jiuyu.replay.generic.bo.words.CruxTypeScaleBo;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;

/**
 * 行业信息项
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-06-20 16:17:24
 */
@Data
@Schema(description = "行业信息项")
public class TradeInfoVo extends TradeVo implements Serializable {
	private static final long serialVersionUID = 1L;

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

	/**
	 * 关键词类型列表
	 */
	@Schema(description = "关键词类型列表")
	private List<CruxTypeScaleBo> cruxTypeScaleBos;

}
