package com.jiuyu.replay.generic.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户资产类型总明细信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "用户资产类型总明细信息")
public class UserPropertyTypeVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@Schema(description = "id")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 父资产类型总明细id
	 */
	@Schema(description = "父资产类型总明细id")
	private Long parentId;
	/**
	 * 用户资产id
	 */
	@Schema(description = "用户资产id")
	private Long propertyId;
	/**
	 * 商品类型id
	 */
	@Schema(description = "商品类型id")
	private Long commodityTypeId;
	/**
	 * 商品类型key
	 */
	@Schema(description = "商品类型key")
	private String commodityTypeCode;
	/**
	 * 商品类型名称
	 */
	@Schema(description = "商品类型名称")
	private String commodityTypeName;
	/**
	 * 商品类型单位
	 */
	@Schema(description = "商品类型单位")
	private String commodityTypeUnit;
	/**
	 * 商品是否要月底清零 0否，1是
	 */
	@Schema(description = "商品是否要月底清零 0否，1是")
	private Integer commodityTypeReset;
	/**
	 * 使用数量
	 */
	@Schema(description = "使用数量")
	private Long useQuantity;
	/**
	 * 总数量
	 */
	@Schema(description = "总数量")
	private Long totalQuantity;
	/**
	 * 总的使用数量
	 */
	@Schema(description = "总的使用数量")
	private Long totalUseQuantity;
	/**
	 * 创建记录时间
	 */
	@Schema(description = "创建记录时间")
	private Date createDate;
	/**
	 * 修改记录时间
	 */
	@Schema(description = "修改记录时间")
	private Date updateDate;
	/**
	 * 是否删除
	 */
	@Schema(description = "是否删除")
	private Integer isDeleted;


}
