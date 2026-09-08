package com.jiuyu.replay.power.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 租户-用户-关联表
 *
 * @author jxy
 * @email 1776764427@qq.com
 * @date 2024-12-31 11:52:17
 */
@Data
@TableName("tb_tenant_user")
public class TenantUserEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 租户id
	 */
	private Long tenantId;
	/**
	 * 用户id
	 */
	private Long userId;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
