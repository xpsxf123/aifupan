package com.jiuyu.replay.generic.vo.order;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 信息项
 *
 * @author lj
 * @email xxxxxxx@qq.com
 * @date 2024-10-10 11:42:33
 */
@Data
@Schema(description = "信息项")
public class OrderDetailInfoVo extends OrderDetailVo implements Serializable {
	private static final long serialVersionUID = 1L;


	@Schema(description = "资产ID-订单定时器")
	private Long propertyId;

	@Schema(description = "用户ID")
	private Long userId;
}
