package com.jiuyu.replay.generic.bo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * AI模型配置表信息
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-27 17:19:05
 */
@Data
@Schema(description = "AI模型配置表信息")
public class AiModelBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 模型编码
	 */
	@Schema(description = "模型编码")
	private String modelCode;
	/**
	 * 来源类型 0：豆包，1：通义，2：DeepSeek
	 */
	@Schema(description = "来源类型 0：豆包，1：通义，2：DeepSeek")
	private Integer resourceType;
	/**
	 * 使用方式 0分析内容，1数据截图的视觉理解
	 */
	@Schema(description = "使用方式 0分析内容，1数据截图的视觉理解")
	private Integer useType;
	/**
	 * 模型名称
	 */
	@Schema(description = "模型名称")
	private String modelName;
	/**
	 * 模型id
	 */
	@Schema(description = "模型id")
	private String endpointId;
	/**
	 * 接口apiKey
	 */
	@Schema(description = "接口apiKey")
	private String apiKey;
	/**
	 * 输出数据流大小(k)
	 */
	@Schema(description = "输出数据流大小(k)")
	private Integer outSize;
	/**
	 * 输入数据流大小(k)
	 */
	@Schema(description = "输入数据流大小(k)")
	private Integer inputSize;
	/**
	 * 缓存上下文大小(k)
	 */
	@Schema(description = "缓存上下文大小(kx)")
	private Integer contextSize;
	/**
	 * 限制使用字数数量
	 */
	@Schema(description = "限制使用字数数量")
	private Integer wordsNum;
	/**
	 * 推荐输出字数
	 */
	@Schema(description = "推荐输出字数")
	private Integer outWordNum;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * 描述
	 */
	@Schema(description = "描述")
	private String remarks;
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
	/**
	 * 是否已删除
	 */
	@Schema(description = "是否已删除")
	private Integer isDeleted;
    /**
     * ai算力消费倍数
     */
    @Schema(description = "ai算力消费倍数")
    private Double consumeMultiple;

}
