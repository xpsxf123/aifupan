package com.jiuyu.replay.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户资产消费记录表信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "用户资产消费记录表信息")
public class UserPropertyDetailsVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	@Schema(description = "")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 用户名称
	 */
	@Schema(description = "用户名称")
	private String userName;
	/**
	 * 父级用户id
	 */
	@Schema(description = "父级用户id")
	private Long parentUserId;
	/**
	 * 订单详情id
	 */
	@Schema(description = "订单详情id")
	private Long orderDetailId;
	/**
	 * 用户资产id
	 */
	@Schema(description = "用户资产id")
	private Long propertyId;
	/**
	 * 使用的资产剩余id
	 */
	@Schema(description = "使用的资产剩余id")
	private Long typeSurplusId;
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
	 * 商品名称
	 */
	@Schema(description = "商品名称")
	private String commodityTypeName;
	/**
	 * 商品单位
	 */
	@Schema(description = "商品单位")
	private String commodityTypeUnit;
	/**
	 * 数量
	 */
	@Schema(description = "数量")
	private Long quantity;
	/**
	 * 操作类型 0：减 1：加
	 */
	@Schema(description = "操作类型 0：减 1：加")
	private Integer signs;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remarks;
	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间")
	private Date createDate;

    /**
     * 资产创建方式 0：用户创建，1：系统创建
     */
    @Schema(description = "资产创建方式 0：用户创建，1：系统创建")
    private Integer assetCreationType;

}
