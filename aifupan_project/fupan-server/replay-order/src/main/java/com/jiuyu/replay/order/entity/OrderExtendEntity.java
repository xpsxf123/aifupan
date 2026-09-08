package com.jiuyu.replay.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import com.jiuyu.replay.common.baseEntity.BaseEntity;
import lombok.Data;

/**
 * 订单的扩展表
 *
 * @author lyw
 * @email 1798883178@qq.com
 * @date 2025-07-05 09:56:24
 */
@Data
@TableName("tb_order_extend")
public class OrderExtendEntity extends BaseEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ID
	 */
	@TableId(value = "id", type = IdType.INPUT)
	private Long id;
	/**
	 * 订单id
	 */
	private Long orderId;
	/**
	 * 付费截图，最多25张图片，图片id之间使用逗号隔开
	 */
	private String payPictures;
	/**
	 * 备注
	 */
	private String remarks;
	/**
	 * 最后修改时间
	 */
	private Date updateDate;
	/**
	 * 创建时间
	 */
	private Date createDate;
	/**
	 * 是否已删除
	 */
	private Integer isDeleted;


}
