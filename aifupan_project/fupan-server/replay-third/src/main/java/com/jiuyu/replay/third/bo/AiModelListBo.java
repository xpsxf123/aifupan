package com.jiuyu.replay.third.bo;


import com.jiuyu.replay.common.bo.PageBo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * AI模型配置表列表查询参数
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Data
@Schema(description = "AI模型配置表列表查询参数")
public class AiModelListBo extends PageBo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 模糊搜索查询条件
	 */
	@Schema(description = "模糊搜索查询条件")
	private String keyword;

	/**
	 * 模型厂商
	 */
	@Schema(description = "模型厂商")
	private String resourceType;

	@Schema(description = "使用方式 0分析内容，1数据截图的视觉理解")
	private Integer useType;

	/**
	 * 上下文大小
	 */
	@Schema(description = "上下文大小")
	private String contextSize;

}
