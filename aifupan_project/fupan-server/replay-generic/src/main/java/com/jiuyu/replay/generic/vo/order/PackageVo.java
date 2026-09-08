package com.jiuyu.replay.generic.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 套餐表(用户版本)信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-08 10:49:32
 */
@Data
@Schema(description = "套餐表(用户版本)信息")
public class PackageVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 套餐名称
	 */
	@Schema(description = "套餐名称")
	private String name;
	/**
	 * 等级，用于套餐升级，只能从低往高升级
	 */
	@Schema(description = "等级，用于套餐升级，只能从低往高升级")
	private Integer level;
	/**
	 * 是否可以压缩0否，1是
	 */
	@Schema(description = "是否可以压缩0否，1是")
	private Integer isCompress;
	/**
	 * 套餐类型(用户只能购买主要套餐)：1主要套餐，2次要套餐
	 */
	@Schema(description = "套餐类型(用户只能购买主要套餐)：1主要套餐，2次要套餐")
	private Integer packageType;
	/**
	 * 是否循环重置用量(如套餐每个月总送xxx)
	 */
	@Schema(description = "是否循环重置用量(如套餐每个月总送xxx)")
	private Integer resetUse;
	/**
	 * 重置时间
	 */
	@Schema(description = "重置时间")
	private Integer resetNum;
	/**
	 * 重置时间单位(0小时，1天，2月，3季度，4半年，5年)
	 */
	@Schema(description = "重置时间单位(0小时，1天，2月，3季度，4半年，5年)")
	private Integer resetUnit;
	/**
	 * 状态 0未上架，1已上架
	 */
	@Schema(description = "状态 0未上架，1已上架")
	private Integer status;
	/**
	 * 是否可以赠送 0否，1是
	 */
	@Schema(description = "是否可以赠送 0否，1是")
	private Integer isGive;
	/**
	 * 版本描述
	 */
	@Schema(description = "版本描述")
	private String description;
	/**
	 * logo图片列表
	 */
	@Schema(description = "logo图片列表")
	private String logoImgs;
	/**
	 * 官网图片
	 */
	@Schema(description = "官网图片")
	private String websiteLogoImages;
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
