package com.jiuyu.replay.order.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户资产信息
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-11 15:04:48
 */
@Data
@Schema(description = "用户资产信息")
public class UserPropertyBo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@Schema(description = "ID")
	private Long id;
	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 当前用户是否使用（0未使用，1使用）
	 */
	@Schema(description = "当前用户是否使用（0未使用，1使用）")
	private Integer isUse;
	/**
	 * 资产id
	 */
	@Schema(description = "资产id")
	private Long parentId;
	/**
	 * 父用户id(当前用户为子账号的时候才会有这个)
	 */
	@Schema(description = "父用户id(当前用户为子账号的时候才会有这个)")
	private Long parentUserId;
	/**
	 * 资产类型 0自己资产，1子用户资产
	 */
	@Schema(description = "资产类型 0自己资产，1子用户资产")
	private Integer type;
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
