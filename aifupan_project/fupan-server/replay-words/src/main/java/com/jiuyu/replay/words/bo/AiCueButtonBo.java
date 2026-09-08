package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 固定提示按钮
信息
 *
 * @author DearSil
 * @email 2437495924@qq.com
 * @date 2025-02-25 11:11:43
 */
@Data
@Schema(description = "固定提示按钮信息")
public class AiCueButtonBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 行业id，0表示全行业
	 */
	@Schema(description = "行业id，0表示全行业")
	private Long tradeId;
	/**
	 * 来源类型 0：系统
	 */
	@Schema(description = "来源类型 0：系统")
	private Integer resourceType;
	/**
	 * 按钮名称
	 */
	@Schema(description = "按钮名称")
	private String buttonName;
	/**
	 * 实际提示词：具体问题
	 */
	@Schema(description = "实际提示词：具体问题")
	private String problem;
	/**
	 * 提示词类型 0: 运营提示词，1:违规提示词 2：对比复盘提示词
	 */
	@Schema(description = "提示词类型 0: 运营提示词，1:违规提示词 2：对比复盘提示词")
	private Integer buttonType;
	/**
	 * 范围 0:全文，1:段落
	 */
	@Schema(description = "范围 0:全文，1:段落")
	private Integer scope;
	/**
	 * 场景 0:直接提示，1:弹框操作
	 */
	@Schema(description = "场景 0:直接提示，1:弹框操作")
	private Integer scene;
	/**
	 * 提示词在当前行业排序
	 */
	@Schema(description = "提示词在当前行业排序")
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


}
