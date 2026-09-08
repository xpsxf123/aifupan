package com.jiuyu.replay.words.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 模型-关键词类型-关联表信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Data
@Schema(description = "模型-关键词类型-关联表信息")
public class ModelCruxVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 数据模型id
	 */
	@Schema(description = "数据模型id")
	private Long modelId;
	/**
	 * 关键词类型id
	 */
	@Schema(description = "关键词类型id")
	private Long cruxTypeId;
	/**
	 * 占比比例值，0.21表示21%
	 */
	@Schema(description = "占比比例值，0.21表示21%")
	private Double scale;
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


}
