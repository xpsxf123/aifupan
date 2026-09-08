package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 资产使用记录关联aitoken消耗表
 *
 * @author lj
 * @email 
 * @date 2025-03-21 18:45:03
 */
@Data
@TableName("tb_property_details_token")
public class PropertyDetailsTokenEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * aiTokenId记录id
	 */
	private Long aiTokenId;
	/**
	 * 资产记录id
	 */
	private Long propertyDetailsId;


}
