package com.jiuyu.replay.words.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 关键词类型信息
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-26 10:44:06
 */
@Data
@Schema(description = "关键词类型信息")
public class CruxTypeBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 关键词类型名称
	 */
	@Schema(description = "关键词类型名称")
	private String name;
	/**
	 * 层级，从1开始
	 */
	@Schema(description = "层级，从1开始")
	private Integer level;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;
	/**
	 * tab的排序
	 */
	@Schema(description = "tab的排序")
	private Integer tabSort;
	/**
	 * 父id
	 */
	@Schema(description = "父id")
	private Long parentId;
	/**
	 * 是否在数据罗盘展示 0：否 1：是
	 */
	@Schema(description = "是否在数据罗盘展示 0：否 1：是")
	private Integer isShowCompass;
	/**
	 * 是否统计到关键词总数 0：否 1：是
	 */
	@Schema(description = "是否统计到关键词总数 0：否 1：是")
	private Integer isCount;
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
